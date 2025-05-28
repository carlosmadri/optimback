package com.airbus.optim.utils;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.Objects;

@Component
public class JwtUtil {

    private static final String EXPECTED_ISSUER = "https://ssobroker-val.airbus.com:10443";
    private static final String EXPECTED_AUDIENCE = "https://v3.airbus.com/FedBroker";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String CLAIM_USERNAME = "sub";
    private static final String CLAIM_EMAIL = "email";
    private static final String ROLE_USER = "ROLE_USER";
    
    private final JwtDecoder jwtDecoder;

    public JwtUtil() {
        var jwkSetUri = "https://your-auth-server.com/jwks";
        jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    public Jwt decodeToken(String token) {
        return jwtDecoder.decode(token);
    }

    public boolean isTokenExpired(String token) {
        return decodeToken(token).getExpiresAt().isBefore(java.time.Instant.now());
    }

    public boolean validateTokenSource(String token) {
        var jwt = decodeToken(token);
        return EXPECTED_ISSUER.equals(jwt.getIssuer().toString()) &&
               EXPECTED_AUDIENCE.equals(jwt.getAudience().get(0));
    }

    public String extractUsername(String token) {
        return decodeToken(token).getClaim(CLAIM_USERNAME);
    }

    public String extractEmail(String token) {
        return decodeToken(token).getClaim(CLAIM_EMAIL);
    }

    public Authentication getAuthentication(String token) {
        var jwt = decodeToken(token);
        var userDetails = new User(jwt.getClaim(CLAIM_USERNAME), "", Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER)));
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        var jwt = decodeToken(token);
        return Objects.equals(jwt.getClaim(CLAIM_USERNAME), userDetails.getUsername()) &&
               jwt.getExpiresAt().isAfter(java.time.Instant.now());
    }

    public String extractToken(HttpServletRequest request) {
        var header = request.getHeader(HEADER_AUTHORIZATION);
        return header != null && header.startsWith(TOKEN_PREFIX) ? header.substring(TOKEN_PREFIX.length()) : null;
    }

    public UserDetails getUserDetails(String token) {
        var jwt = decodeToken(token);
        return new User(jwt.getClaim(CLAIM_USERNAME), "", Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER)));
    }
}

