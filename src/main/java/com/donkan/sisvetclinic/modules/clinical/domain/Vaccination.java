package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Vaccination(
    long id,
    long petId,
    Long consultationId,
    Long attentionId,
    String vaccine,
    String dose,
    LocalDateTime appliedAt,
    LocalDate nextDoseDate,
    long veterinarianId,
    Long productId,
    Integer quantityUsed
) {
    public Vaccination(
        long id,
        long petId,
        Long consultationId,
        Long attentionId,
        String vaccine,
        String dose,
        LocalDateTime appliedAt,
        LocalDate nextDoseDate,
        long veterinarianId
    ) {
        this(id, petId, consultationId, attentionId, vaccine, dose, appliedAt, nextDoseDate, veterinarianId, null, null);
    }
}
