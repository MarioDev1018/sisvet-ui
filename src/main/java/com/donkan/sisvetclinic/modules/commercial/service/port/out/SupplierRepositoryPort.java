package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepositoryPort {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long supplierId);

    Optional<Supplier> findByRuc(String ruc);

    List<Supplier> findAll();

    boolean existsByRuc(String ruc);
}