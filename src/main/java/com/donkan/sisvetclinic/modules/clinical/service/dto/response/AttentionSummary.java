package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AttentionSummary(
    long id,
    long petId,
    String petName,
    String species,
    String speciesLabel,
    long ownerId,
    String ownerName,
    String ownerDocument,
    String type,
    Integer priority,
    String urgencyDescription,
    Long priorityAssignedBy,
    String state,
    LocalDateTime arrivedAt,
    boolean requiresGrooming,
    boolean clinicalClosed,
    Long consultationId,
    boolean vaccinationCompleted,
    BigDecimal lastWeightKg,
    long version
) {}
