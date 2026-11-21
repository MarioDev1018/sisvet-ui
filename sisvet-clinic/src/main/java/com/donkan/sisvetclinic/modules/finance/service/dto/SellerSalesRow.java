package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record SellerSalesRow(
        Long userId,
        String sellerName,
        int saleCount,
        BigDecimal total,
        BigDecimal averageTicket
) {
}