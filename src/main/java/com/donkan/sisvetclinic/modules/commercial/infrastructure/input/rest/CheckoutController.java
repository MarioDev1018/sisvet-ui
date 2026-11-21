package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.CheckoutResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.CheckoutUseCase;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkouts")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
public class CheckoutController {

    private final CheckoutUseCase checkouts;

    public CheckoutController(CheckoutUseCase checkouts) {
        this.checkouts = checkouts;
    }

    @PostMapping
    public CheckoutResult start(
            @AuthenticationPrincipal AuthResult user,
            @RequestBody SaleRequest request) {

        return checkouts.start(requireUserId(user), request);
    }

    @GetMapping("/{operationKey}")
    public CheckoutResult find(
            @AuthenticationPrincipal AuthResult user,
            @PathVariable String operationKey) {

        return checkouts.find(requireUserId(user), operationKey);
    }

    @PostMapping("/{operationKey}/verify")
    public CheckoutResult verify(
            @AuthenticationPrincipal AuthResult user,
            @PathVariable String operationKey) {

        return checkouts.verify(requireUserId(user), operationKey);
    }

    @PostMapping("/{operationKey}/credentials")
    public CheckoutResult submitCredentials(
            @AuthenticationPrincipal AuthResult user,
            @PathVariable String operationKey,
            @RequestBody CheckoutCredentials credentials) {

        return checkouts.submitCredentials(requireUserId(user), operationKey, credentials);
    }

    @PostMapping("/{operationKey}/cancel-recovery")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CheckoutResult cancelRecovery(
            @AuthenticationPrincipal AuthResult user,
            @PathVariable String operationKey) {

        return checkouts.cancelForRecovery(requireUserId(user), operationKey);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(exception, "El recurso solicitado no existe."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(errorBody(exception, "La solicitud es inválida."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(exception, "No se pudo completar la operación."));
    }

    private Long requireUserId(AuthResult user) {
        if (user == null || user.id() == null) {
            throw new IllegalStateException("No existe un usuario autenticado.");
        }
        return user.id();
    }

    private Map<String, String> errorBody(RuntimeException exception, String fallback) {
        String msg = exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : fallback;
        return Map.of("message", msg, "error", msg);
    }
}