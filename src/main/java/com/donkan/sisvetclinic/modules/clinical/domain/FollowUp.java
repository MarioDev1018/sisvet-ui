package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record FollowUp(
    long id,
    long petId,
    Long consultationId,
    String reason,
    LocalDateTime dueAt,
    String status,
    LocalDateTime reviewedAt,
    Long appointmentId
) {}
