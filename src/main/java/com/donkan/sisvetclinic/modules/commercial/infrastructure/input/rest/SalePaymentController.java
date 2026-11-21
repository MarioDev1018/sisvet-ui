
package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SalePaymentUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort.PaymentReceipt;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/{saleId}/payments")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
public class SalePaymentController {

    private final SalePaymentUseCase payments;

    public SalePaymentController(SalePaymentUseCase payments) {
        this.payments = payments;
    }

    @PostMapping("/{provider}")
    public PaymentReceipt initiate(
            @PathVariable Long saleId,
            @PathVariable String provider) {

        return payments.initiateForSale(saleId, provider);
    }

    @GetMapping("/{paymentId}")
    public PaymentReceipt find(
            @PathVariable Long saleId,
            @PathVariable String paymentId) {

        return payments.findForSale(saleId, paymentId);
    }

    @PostMapping("/{paymentId}/credentials")
    public PaymentReceipt credentials(
            @PathVariable Long saleId,
            @PathVariable String paymentId,
            @RequestBody CheckoutCredentials credentials) {

        return payments.submitCredentialsForSale(saleId, paymentId, credentials);
    }

    @PostMapping("/{paymentId}/verify")
    public PaymentReceipt verify(
            @PathVariable Long saleId,
            @PathVariable String paymentId) {

        return payments.verifyForSale(saleId, paymentId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(exception, "El recurso de pago no existe."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(errorBody(exception, "La solicitud de pago es inválida."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(exception, "No se pudo procesar el pago."));
    }

    private Map<String, String> errorBody(RuntimeException exception, String fallback) {
        String msg = exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : fallback;
        return Map.of("message", msg, "error", msg);
    }
}
