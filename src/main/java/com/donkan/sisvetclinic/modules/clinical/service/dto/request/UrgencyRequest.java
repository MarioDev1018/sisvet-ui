package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record UrgencyRequest(
    long petId,
    String description
) {}
