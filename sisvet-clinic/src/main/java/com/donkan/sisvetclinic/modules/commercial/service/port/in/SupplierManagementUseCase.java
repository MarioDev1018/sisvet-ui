package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.Supplier;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SupplierRequest;

import java.util.List;

public interface SupplierManagementUseCase {

    Supplier registerSupplier(SupplierRequest request);

    Supplier findByRuc(String ruc);

    List<Supplier> listSuppliers();
}