package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record ProductSalesRow(
        String productCode,
        String productName,
        String category,
        String baseUnit,
        long quantitySold,
        BigDecimal averagePrice,
        BigDecimal total
) {
}