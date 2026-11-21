package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.math.BigDecimal;

public record ConsultationRequest(
    long attentionId,
    String reason,
    BigDecimal weight,
    String symptoms,
    String observations
) {}
