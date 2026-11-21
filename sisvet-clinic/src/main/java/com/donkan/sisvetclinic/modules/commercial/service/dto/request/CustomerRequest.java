package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

public record CustomerRequest(
        String documentNumber,
        String documentType,
        String fullName,
        String phone,
        String email,
        String address
) {
}