package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record ProductPerformanceRow(
        String productCode,
        String productName,
        String category,
        String baseUnit,
        long purchasedQuantity,
        BigDecimal averageCost,
        BigDecimal purchaseTotal,
        long soldQuantity,
        BigDecimal averageSalePrice,
        BigDecimal salesAmount,
        BigDecimal estimatedMargin,
        int stock
) {
}