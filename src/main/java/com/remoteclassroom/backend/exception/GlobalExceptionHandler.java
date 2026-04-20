package com.remoteclassroom.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =============================================
    // 🔐 AUTH ERRORS — Return 401 (not 500)
    // =============================================
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleAuthException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // =============================================
    // 🚫 NOT AUTHENTICATED — Return 401
    // =============================================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "You must be logged in to access this resource.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // =============================================
    // 🚫 ACCESS DENIED — Return 403 (not 500!)
    // =============================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Access denied. You do not have permission to perform this action.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // =============================================
    // ⚠️ RUNTIME ERRORS — e.g. "User not found", "Invalid password"
    // =============================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        System.err.println("❌ Runtime Exception: " + message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    // =============================================
    // ✅ VALIDATION ERRORS — Return 400 with JSON
    // =============================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);

        if ("WEAK_PASSWORD".equals(errorMessage)) {
            body.put("error", "Password too weak. Try: " + generateStrongPassword());
        } else {
            body.put("error", "Invalid input. Please check your details.");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // =============================================
    // 🔥 LAST RESORT — Catch any unexpected Exception
    // =============================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "An internal error occurred. Please try again later.");
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String generateStrongPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%!";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}