package com.airbus.optim.configuration;

import com.airbus.optim.filter.TokenValidationFilter;
import com.airbus.optim.utils.TokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;
    private static final String AUTH_PATH = "/auth/**";
    private static final String ERROR_GENERIC_PATH = "/error/**";
    private static final String HEALTH_CHECK_WHITELIST = "/api/optim/actuator/**";
    private static final String ERROR_PATH = "/error/403";
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE");
    private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");
    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"};

    private final TokenValidator tokenValidator;

    public SecurityConfig(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AUTH_PATH).permitAll()
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers(new AntPathRequestMatcher(HEALTH_CHECK_WHITELIST)).permitAll()
                .requestMatchers(new AntPathRequestMatcher(ERROR_GENERIC_PATH)).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        response.sendRedirect(ERROR_PATH);
                    }
                })
            );

        http.addFilterBefore(new TokenValidationFilter(tokenValidator), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}