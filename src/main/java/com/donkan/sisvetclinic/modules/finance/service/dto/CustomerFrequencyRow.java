package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record CustomerFrequencyRow(
        String documentNumber,
        String customerName,
        int purchaseCount,
        BigDecimal totalSpent,
        BigDecimal averageTicket
) {
}