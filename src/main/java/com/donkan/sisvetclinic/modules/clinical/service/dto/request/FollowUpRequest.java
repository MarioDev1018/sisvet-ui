package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.time.LocalDateTime;

public record FollowUpRequest(
    long petId,
    Long consultationId,
    String reason,
    LocalDateTime dueAt,
    Long appointmentId
) {}
