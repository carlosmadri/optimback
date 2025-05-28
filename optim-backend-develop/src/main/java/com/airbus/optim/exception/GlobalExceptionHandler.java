package com.airbus.optim.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.AuthenticationException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_UNAUTHORIZED = "No autorizado";
    private static final String ERROR_FORBIDDEN = "Acceso denegado";
    private static final String ERROR_NOT_FOUND = "Recurso no encontrado";
    private static final String ERROR_BAD_REQUEST = "Petición incorrecta";
    private static final String ERROR_SESSION_EXPIRED = "Sesión expirada";
    private static final String ERROR_INTERNAL_SERVER = "Error interno del servidor";
    private static final String MESSAGE_REDIRECT_LOGIN = "Redirigiendo a login...";
    private static final String MESSAGE_UNAUTHORIZED_PREFIX = "No autorizado: ";

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", ERROR_UNAUTHORIZED,
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", ERROR_FORBIDDEN,
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", ERROR_NOT_FOUND,
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ERROR_BAD_REQUEST,
                "message", ex.getMessage()
        ));
    }

    public ResponseEntity<Map<String, String>> redirectToLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", ERROR_SESSION_EXPIRED,
                "message", MESSAGE_REDIRECT_LOGIN
        ));
    }

    public void sendUnauthorizedResponse(HttpServletResponse response, String message) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        try {
            response.getWriter().write(MESSAGE_UNAUTHORIZED_PREFIX + message);
            response.getWriter().flush();
        } catch (IOException e) {
            System.err.println("Error al escribir en el response: " + e.getMessage());
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", ERROR_INTERNAL_SERVER,
                "message", ex.getMessage()
        ));
    }
}
