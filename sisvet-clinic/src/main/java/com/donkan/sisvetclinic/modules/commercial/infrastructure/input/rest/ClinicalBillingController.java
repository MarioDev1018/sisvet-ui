package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalCharge;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.ClinicalPaymentRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.ClinicalBillingUseCase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clinical-billing")
public class ClinicalBillingController {

    private final ClinicalBillingUseCase billing;

    public ClinicalBillingController(ClinicalBillingUseCase billing) {
        this.billing = billing;
    }

    @GetMapping("/consultations/{consultationId}/charges")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR', 'RECEPCIONISTA')")
    public List<ClinicalCharge> getPendingCharges(@PathVariable Long consultationId) {
        return billing.getPendingCharges(consultationId);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public ResponseEntity<SaleResult> registerPayment(
            @AuthenticationPrincipal AuthResult user,
            @RequestBody ClinicalPaymentRequest request) {

        if (user == null) {
            throw new IllegalStateException("No existe un usuario autenticado.");
        }

        SaleResult result = billing.registerClinicalPayment(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}