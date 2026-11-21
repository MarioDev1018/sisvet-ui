package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(
    basePackages = "com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest"
)
public class ClinicalErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(
        IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
            errorBody(exception, "Solicitud clínica inválida")
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(
        IllegalStateException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            errorBody(exception, "No se pudo completar la operación clínica")
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> forbidden(
        SecurityException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            errorBody(exception, "No tiene permisos para esta operación clínica")
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integrity(
        DataIntegrityViolationException exception
    ) {
        String msg = "Operación en conflicto con restricciones de datos";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            Map.of("error", msg, "message", msg)
        );
    }

    private Map<String, String> errorBody(RuntimeException exception, String fallback) {
        String msg = exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()
            ? exception.getMessage()
            : fallback;
        return Map.of("error", msg, "message", msg);
    }
}
