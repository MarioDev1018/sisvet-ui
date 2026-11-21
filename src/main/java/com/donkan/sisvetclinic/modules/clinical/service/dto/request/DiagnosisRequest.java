package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record DiagnosisRequest(
    long consultationId,
    String description,
    String indications
) {}
