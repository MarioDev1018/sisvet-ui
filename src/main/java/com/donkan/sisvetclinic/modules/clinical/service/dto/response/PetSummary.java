package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetSummary(
    long id,
    long ownerId,
    String ownerName,
    String ownerDocument,
    String ownerPhone,
    String name,
    String species,
    String speciesLabel,
    String breed,
    String sex,
    LocalDate birthDate,
    Integer estimatedAgeMonths,
    BigDecimal lastWeightKg,
    LocalDateTime lastWeightAt,
    Long activeAttentionId,
    String activeAttentionState,
    String activeAttentionType
) {}
