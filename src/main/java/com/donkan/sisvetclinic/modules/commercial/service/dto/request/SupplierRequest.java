package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

public record SupplierRequest(
        String ruc,
        String businessName,
        String phone,
        String email
) {
}