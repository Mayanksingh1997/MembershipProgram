package com.firstclub.firstclub.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.dto.response.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.builder()
                .status("ERROR")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build());
    }

    @ExceptionHandler(MembershipException.class)
    public ResponseEntity<ErrorResponse> handleMembershipException(MembershipException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.builder()
                .status("ERROR")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ResponseEntity<ErrorResponse> invalidEnumResponse = mapInvalidEnumError(ex);
        if (invalidEnumResponse != null) {
            return invalidEnumResponse;
        }
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status("ERROR")
                .message("Malformed request body")
                .errorCode("VALIDATION_ERROR")
                .build());
    }

    private ResponseEntity<ErrorResponse> mapInvalidEnumError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormat) {
                if (invalidFormat.getTargetType() == PlanCode.class) {
                    return notFoundResponse("Plan not found: " + invalidFormat.getValue(), "PLAN_NOT_FOUND");
                }
                if (invalidFormat.getTargetType() == TierCode.class) {
                    return notFoundResponse("Tier not found: " + invalidFormat.getValue(), "TIER_NOT_FOUND");
                }
            }
            if (current.getClass().getName().endsWith("InvalidFormatException")) {
                String message = current.getMessage();
                if (message != null) {
                    if (message.contains(PlanCode.class.getName())) {
                        return notFoundResponse("Plan not found: " + extractInvalidEnumValue(message), "PLAN_NOT_FOUND");
                    }
                    if (message.contains(TierCode.class.getName())) {
                        return notFoundResponse("Tier not found: " + extractInvalidEnumValue(message), "TIER_NOT_FOUND");
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private String extractInvalidEnumValue(String message) {
        int fromStringIndex = message.indexOf("from String \"");
        if (fromStringIndex < 0) {
            return "unknown";
        }
        int valueStart = fromStringIndex + "from String \"".length();
        int valueEnd = message.indexOf('\"', valueStart);
        if (valueEnd < 0) {
            return "unknown";
        }
        return message.substring(valueStart, valueEnd);
    }

    private ResponseEntity<ErrorResponse> notFoundResponse(String message, String errorCode) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .status("ERROR")
                .message(message)
                .errorCode(errorCode)
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status("ERROR")
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .build());
    }

    @ExceptionHandler({OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .status("ERROR")
                .message("Concurrent modification detected. Please retry.")
                .errorCode("CONCURRENT_MODIFICATION")
                .build());
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ErrorResponse> handleDataAccessException(Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.builder()
                .status("ERROR")
                .message("Database operation failed")
                .errorCode("DATABASE_ERROR")
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .status("ERROR")
                .message("An unexpected error occurred")
                .errorCode("INTERNAL_ERROR")
                .build());
    }
}
