package com.orangemantra.adminservice.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Downstream service error: status={}, message={}", ex.status(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", "Downstream call failed: " + sanitize(ex.getMessage()));
        return ResponseEntity.status(status).body(body);
    }

    private String sanitize(String msg) {
        if (msg == null) return null;
        return msg.replaceAll("(Bearer )([A-Za-z0-9-_.]+)", "$1***");
    }
}
