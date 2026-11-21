package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.Supplier;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SupplierRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SupplierManagementUseCase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierManagementUseCase suppliers;

    public SupplierController(SupplierManagementUseCase suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    public List<Supplier> listSuppliers() {
        return suppliers.listSuppliers();
    }

    @GetMapping("/search")
    public Supplier findByRuc(@RequestParam String ruc) {
        return suppliers.findByRuc(ruc);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACEN')")
    public ResponseEntity<Supplier> registerSupplier(@RequestBody SupplierRequest request) {

        Supplier supplier = suppliers.registerSupplier(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }
}