package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.PurchaseRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.PurchaseResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.PurchaseUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACEN')")
public class PurchaseController {

    private final PurchaseUseCase purchases;

    public PurchaseController(PurchaseUseCase purchases) {
        this.purchases = purchases;
    }

    @PostMapping
    public ResponseEntity<PurchaseResult> registerPurchase(
            @AuthenticationPrincipal AuthResult user,
            @RequestBody PurchaseRequest request) {

        if (user == null) {
            throw new IllegalStateException("No existe un usuario autenticado.");
        }

        PurchaseResult result = purchases.registerPurchase(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}