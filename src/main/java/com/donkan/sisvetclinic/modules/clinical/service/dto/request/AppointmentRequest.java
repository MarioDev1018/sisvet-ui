package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.time.LocalDateTime;

public record AppointmentRequest(
    long petId,
    long professionalId,
    String reason,
    LocalDateTime date,
    int durationMinutes,
    String specialty,
    Long consultationId
) {}
