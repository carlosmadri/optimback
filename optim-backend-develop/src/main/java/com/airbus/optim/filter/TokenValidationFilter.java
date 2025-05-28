package com.airbus.optim.filter;

import com.airbus.optim.utils.TokenValidator;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Order(1)
public class TokenValidationFilter extends OncePerRequestFilter {

    private final TokenValidator tokenValidator;

    public TokenValidationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            var header = request.getHeader("Authorization");

            if (Objects.isNull(header) || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            var token = header.substring(7);

            boolean isValid = tokenValidator.validateTokenStructure(token) &&
                              tokenValidator.validateTokenSource(token);

            if (!isValid) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }

            var userDetails = tokenValidator.getUserDetails(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno en la validación de tokens");
        }
    }
}
