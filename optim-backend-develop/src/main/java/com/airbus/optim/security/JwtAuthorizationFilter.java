package com.airbus.optim.security;

import com.airbus.optim.utils.JwtUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String ERROR_TOKEN_UNAUTHORIZED = "Token no autorizado";

    private static final List<String> HEALTH_CHECK_WHITELIST = List.of(
            "/api/optim/actuator",
            "/api/optim/actuator/**",
            "/api/optim/actuator/health",
            "/error");

    private final JwtUtil jwtUtil;

    public JwtAuthorizationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if(HEALTH_CHECK_WHITELIST.stream().anyMatch(path::startsWith)){
            filterChain.doFilter(request, response);
        }

        var token = jwtUtil.extractToken(request);

        if (Objects.isNull(token) || !jwtUtil.validateTokenSource(token)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ERROR_TOKEN_UNAUTHORIZED);
            return;
        }

        var userDetails = jwtUtil.getUserDetails(token);
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}