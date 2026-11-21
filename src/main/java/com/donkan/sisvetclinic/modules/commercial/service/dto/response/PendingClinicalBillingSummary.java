package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

import java.math.BigDecimal;

public record PendingClinicalBillingSummary(
    Long consultationId,
    Long attentionId,
    Long petId,
    String petName,
    String species,
    Long ownerId,
    String ownerName,
    String ownerDocument,
    String attentionState,
    Integer pendingCount,
    BigDecimal pendingTotal
) {}
