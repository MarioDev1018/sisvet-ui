package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record Appointment(
    long id,
    long petId,
    long professionalId,
    String reason,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String status,
    String specialty,
    Long consultationId,
    LocalDateTime createdAt,
    boolean reminderSent
) {}
