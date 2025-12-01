package com.capstone_project.elderly_platform.exceptions;

import com.capstone_project.elderly_platform.dtos.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> unauthorizedException(AuthenticationException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(new Date(), "Failed",
                "You do not have access", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(ElementNotFoundException.class)
    public ResponseEntity<ErrorResponse> elementNotFoundException(ElementNotFoundException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed", "Not found",
                e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(ElementExistException.class)
    public ResponseEntity<ErrorResponse> elementExistException(ElementExistException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Element already exists", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDeniedException(AccessDeniedException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(new Date(), "Failed", "You do not have permission to access this API",
                        e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(UnchangedStateException.class)
    public ResponseEntity<ErrorResponse> unchangedStateException(UnchangedStateException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Element unchanged", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResourceFoundException(NoResourceFoundException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(new Date(), "Failed",
                "Resource not found", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> unauthorizedExceptionJWT(ExpiredJwtException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(new Date(), "Failed",
                "You do not have access", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Some fields are invalid", errors, request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(ParseEnumException.class)
    public ResponseEntity<ErrorResponse> parseEnumException(ParseEnumException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Cannot convert enum", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFound(EntityNotFoundException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Object not found", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequestException(BadRequestException e, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new Date(), "Failed",
                "Invalid request", e.getMessage(), request.getDescription(false).replace("uri=", "")));
    }
}