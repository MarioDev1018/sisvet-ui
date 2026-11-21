package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Supplier;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SupplierRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SupplierManagementUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SupplierRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;

public class SupplierManagementService implements SupplierManagementUseCase {

    private final SupplierRepositoryPort suppliers;

    public SupplierManagementService(SupplierRepositoryPort suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    @Transactional
    public Supplier registerSupplier(SupplierRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Los datos del proveedor son obligatorios.");
        }

        String ruc = CommercialValidation.requireText(request.ruc(), "El RUC");
        String businessName = CommercialValidation.requireText(request.businessName(), "La razón social");

        if (!ruc.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener once dígitos.");
        }

        if (suppliers.existsByRuc(ruc)) {
            throw new IllegalStateException("El RUC ya está registrado.");
        }

        Supplier supplier = new Supplier();

        supplier.setRuc(ruc);
        supplier.setBusinessName(businessName);
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setActive(true);

        return suppliers.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier findByRuc(String ruc) {

        String value = CommercialValidation.requireText(ruc, "El RUC");

        return suppliers.findByRuc(value)
                .filter(Supplier::isActive)
                .orElseThrow(() -> new NoSuchElementException("El proveedor no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers() {
        return suppliers.findAll().stream().filter(Supplier::isActive).toList();
    }
}