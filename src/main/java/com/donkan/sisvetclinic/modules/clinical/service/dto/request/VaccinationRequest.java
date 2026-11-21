package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VaccinationRequest(
    long petId,
    Long consultationId,
    String vaccine,
    String dose,
    LocalDate nextDoseDate,
    Long productId,
    Integer quantityUsed,
    BigDecimal applicationFee
) {
    public VaccinationRequest(
        long petId,
        Long consultationId,
        String vaccine,
        String dose,
        LocalDate nextDoseDate
    ) {
        this(petId, consultationId, vaccine, dose, nextDoseDate, null, null, null);
    }
}
