package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record TreatmentRequest(
    long consultationId,
    String description,
    String instructions,
    Long productId
) {}
