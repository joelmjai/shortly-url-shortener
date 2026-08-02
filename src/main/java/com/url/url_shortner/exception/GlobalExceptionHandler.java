package com.url.url_shortner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Turns bad client input into clean 400 responses across the whole API
 * instead of letting it surface as an opaque 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Missing required query params, e.g. startDate/endDate on the analytics endpoints.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return badRequest("Missing required parameter: " + ex.getParameterName());
    }

    // Unparseable dates passed to the analytics endpoints.
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> handleBadDate(DateTimeParseException ex) {
        return badRequest("Invalid date format. Use ISO format (e.g. 2026-08-02 or 2026-08-02T10:15:30).");
    }

    // Missing or malformed JSON request body.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("Malformed or missing request body.");
    }

    // Invalid arguments, e.g. registering without a password.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest(ex.getMessage() == null ? "Invalid request." : ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", message
        ));
    }
}
