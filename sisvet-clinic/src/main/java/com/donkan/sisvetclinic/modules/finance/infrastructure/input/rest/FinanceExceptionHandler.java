package com.donkan.sisvetclinic.modules.finance.infrastructure.input.rest;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        ExpenseController.class,
        ReportController.class
})
public class FinanceExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> dataConflict(DataIntegrityViolationException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "La operación incumple una restricción de la base de datos."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> databaseError(DataAccessException exception) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "No se pudo completar la operación en la base de datos."));
    }
}