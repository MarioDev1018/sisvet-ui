package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.time.LocalDateTime;

public record AppointmentView(
    long id,
    long petId,
    String petName,
    String speciesLabel,
    long ownerId,
    String ownerName,
    long professionalId,
    String professionalName,
    String reason,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String status,
    String specialty,
    Long consultationId,
    LocalDateTime createdAt,
    boolean reminderSent
) {}
