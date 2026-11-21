package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseReportRow(
        Long purchaseId,
        LocalDateTime purchaseDatetime,
        String employee,
        String supplier,
        String paymentMethod,
        BigDecimal total
) {
}