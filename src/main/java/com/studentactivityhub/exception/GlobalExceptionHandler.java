package com.studentactivityhub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(
            ResourceNotFoundException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(
            NoResourceFoundException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "Resource not found.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(
            IllegalStateException exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(
            Exception exception) {

        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}