package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.time.LocalDateTime;

public record FollowUpView(
    long id,
    long petId,
    String petName,
    String speciesLabel,
    long ownerId,
    String ownerName,
    String ownerPhone,
    Long consultationId,
    String reason,
    LocalDateTime dueAt,
    String status,
    LocalDateTime reviewedAt,
    Long appointmentId
) {}
