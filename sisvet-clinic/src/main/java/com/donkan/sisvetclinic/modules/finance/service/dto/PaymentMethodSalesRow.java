package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record PaymentMethodSalesRow(
        String paymentMethod,
        int saleCount,
        BigDecimal total
) {
}