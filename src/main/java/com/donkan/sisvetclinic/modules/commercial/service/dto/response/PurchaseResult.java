package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseResult(
        Long purchaseId,
        BigDecimal totalAmount,
        LocalDateTime purchaseDatetime,
        String status
) {
}