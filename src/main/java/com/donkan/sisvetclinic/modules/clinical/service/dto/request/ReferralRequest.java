package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

public record ReferralRequest(
    long consultationId,
    String reason,
    String destination
) {}
