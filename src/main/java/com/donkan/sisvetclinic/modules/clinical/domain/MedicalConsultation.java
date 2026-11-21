package com.donkan.sisvetclinic.modules.clinical.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MedicalConsultation(
    long id,
    long attentionId,
    long petId,
    long veterinarianId,
    String reason,
    BigDecimal weight,
    String symptoms,
    String observations,
    String status,
    LocalDateTime createdAt
) {}
