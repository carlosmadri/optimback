package com.airbus.optim.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;

@Component
public class TokenValidator {

    @Value("${app.validateWithCertificate}")
    private boolean VALIDATE_WITH_CERTIFICATE;

    private static final boolean VALIDATE_TOKEN_SOURCE = false;
    private static final String EXPECTED_ISSUER = "https://ssobroker-val.airbus.com:10443";
    private static final String JWKS_URL = "https://ssobroker-val.airbus.com:10443/ext/JWKS/STS";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String CLAIM_SUB = "sub";
    private static final String CLAIM_ISS = "iss";
    private static final String TOKEN_PARTS_SEPARATOR = "\\.";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final JwtDecoder jwtDecoder;
    private final RSAPublicKey publicKey;

    public TokenValidator() {
        try {
            if (VALIDATE_WITH_CERTIFICATE) {
                this.publicKey = fetchJWKS();
                if (this.publicKey == null) {
                    throw new IllegalStateException("Failed to load the public key.");
                }
                this.jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            } else {
                this.publicKey = null;
                this.jwtDecoder = null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize TokenValidator", e);
        }
    }

    private RSAPublicKey fetchJWKS() {
        if (!VALIDATE_WITH_CERTIFICATE) {
            return null;
        }

        try {
            var url = new URL(JWKS_URL);
            var conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            var response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            var jwksJson = new JSONObject(response.toString());
            var keys = jwksJson.getJSONArray("keys");

            if (keys.isEmpty()) {
                throw new IllegalStateException("No keys found in JWKS.");
            }

            var key = keys.getJSONObject(0);
            var modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.getString("n")));
            var exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.getString("e")));

            var spec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to process the public key.", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            if (!validateTokenStructure(token)) return false;
            if (!validateTokenSource(token)) return false;

            var userDetails = getUserDetails(token);
            if (userDetails == null) {
                throw new AuthenticationException("UserDetails is null, authentication failed.") {};
            }

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

            return true;
        } catch (Exception e) {
            throw new AuthenticationException("Unexpected failure in validateToken.", e) {};
        }
    }

    public boolean validateTokenSource(String token) {
        if (!VALIDATE_TOKEN_SOURCE) {
            return true;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            return EXPECTED_ISSUER.equals(jwt.getClaim(CLAIM_ISS));
        } catch (Exception e) {
            throw new AuthenticationException("Token source validation failed.", e) {};
        }
    }

    public UserDetails getUserDetails(String token) {
        if (!VALIDATE_WITH_CERTIFICATE) {
            return new User("anonymous", "", Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER)));
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            var username = Optional.ofNullable(jwt.getClaim(CLAIM_SUB))
                                   .map(Object::toString)
                                   .orElse("anonymous");
            return new User(username, "", Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER)));
        } catch (Exception e) {
            throw new AuthenticationException("Failed to retrieve user details.", e) {};
        }
    }

    public boolean validateTokenStructure(String token) {
        return Optional.ofNullable(token)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.split(TOKEN_PARTS_SEPARATOR))
                .filter(parts -> parts.length == 3)
                .map(parts -> {
                    try {
                        Base64.getUrlDecoder().decode(parts[0]);
                        Base64.getUrlDecoder().decode(parts[1]);
                        return true;
                    } catch (IllegalArgumentException e) {
                        throw new AuthenticationException("Invalid token structure.", e) {};
                    }
                })
                .orElseThrow(() -> new AuthenticationException("Token structure validation failed.") {});
    }

    public String extractUsername(String token) {
        try {
            token = token.replace("Bearer ", "").trim();

            var tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new AuthenticationException("Malformed token.") {};
            }

            var payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
            var payload = new JSONObject(payloadJson);

            if (VALIDATE_WITH_CERTIFICATE) {
                var expirationTime = payload.optLong("exp", 0);
                var currentTime = System.currentTimeMillis() / 1000;

                if (expirationTime > 0 && expirationTime < currentTime) {
                    throw new AuthenticationException("Token has expired.") {};
                }
            }

            return Optional.ofNullable(payload.optString("email", null))
                           .filter(username -> !username.isEmpty())
                           .orElseThrow(() -> new AuthenticationException("Missing 'email' claim in token.") {});
        } catch (Exception e) {
            throw new AuthenticationException("Failed to extract username from token.", e) {};
        }
    }
}