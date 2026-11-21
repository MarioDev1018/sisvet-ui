package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.port.in.InvoiceUseCase;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
public class InvoiceController {

    private final InvoiceUseCase invoices;

    public InvoiceController(InvoiceUseCase invoices) {
        this.invoices = invoices;
    }

    @PostMapping
    public Object issueInvoice(@RequestBody Map<String, Long> request) {

        throw new UnsupportedOperationException(
                "La emisión SUNAT no está habilitada hasta configurar un proveedor real."
        );
    }

    @GetMapping("/{invoiceId}/status")
    public Map<String, String> checkStatus(@PathVariable Long invoiceId) {

        String status = invoices.checkInvoiceStatus(invoiceId);

        return Map.of("status", status);
    }
}