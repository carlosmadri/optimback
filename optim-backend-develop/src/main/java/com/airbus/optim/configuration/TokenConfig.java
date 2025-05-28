package com.airbus.optim.configuration;

import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.Base64;

@Configuration
public class TokenConfig {

    private static final String INTROSPECTION_URL = "https://ssobroker-varl.airbus.com:10443/as/intrsopect.oauth2";
    private static final String CLIENT_ID = "CLI_9DD8_OPTIM-DEV-VAL-V";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CLIENT_ID = "Client-Id";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public boolean introspectToken(String token) {
        return validateToken(token);
    }

    private boolean validateToken(String token) {
        JSONObject payload = decodeTokenWithoutValidation(token);
        if (payload == null) {
            return false;
        }

        long exp = payload.optLong("exp", 0);
        if (exp < System.currentTimeMillis() / 1000) {
            return false;
        }

        return callIntrospectionService(token);
    }

    private JSONObject decodeTokenWithoutValidation(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return new JSONObject(payloadJson);

        } catch (Exception e) {
            return null;
        }
    }

    private boolean callIntrospectionService(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_AUTHORIZATION, TOKEN_PREFIX + token);
        headers.set(HEADER_CLIENT_ID, CLIENT_ID);
        headers.set(HEADER_CONTENT_TYPE, "application/json");
        headers.set(HEADER_ACCEPT, "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate().exchange(INTROSPECTION_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                return jsonResponse.optBoolean("active", false);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}



