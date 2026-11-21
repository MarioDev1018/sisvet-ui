package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record ProcedureRequest(
    long consultationId,
    String name,
    String observations
) {}
