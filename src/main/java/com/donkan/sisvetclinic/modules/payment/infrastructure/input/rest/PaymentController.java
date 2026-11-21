
package com.donkan.sisvetclinic.modules.payment.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.payment.service.dto.response.PaymentResult;
import com.donkan.sisvetclinic.modules.payment.service.port.in.PaymentUseCase;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
public class PaymentController {

    private final PaymentUseCase payments;

    public PaymentController(PaymentUseCase payments) {
        this.payments = payments;
    }

    @GetMapping("/{paymentId}")
    public PaymentResult findPayment(@PathVariable String paymentId) {
        return payments.findPayment(paymentId);
    }

    @PostMapping("/{paymentId}/verify")
    public PaymentResult verifyPayment(@PathVariable String paymentId) {
        return payments.verifyPayment(paymentId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(exception, "El pago solicitado no existe."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(errorBody(exception, "La solicitud de pago no es válida."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody(exception, "El pago no pudo actualizarse en el estado actual."));
    }

    private Map<String, String> errorBody(RuntimeException exception, String fallback) {
        String msg = exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : fallback;
        return Map.of("message", msg, "error", msg);
    }
}
