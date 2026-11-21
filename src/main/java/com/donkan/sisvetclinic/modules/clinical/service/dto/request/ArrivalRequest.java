package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record ArrivalRequest(
    long petId,
    String type,
    boolean requiresGrooming
) {}
