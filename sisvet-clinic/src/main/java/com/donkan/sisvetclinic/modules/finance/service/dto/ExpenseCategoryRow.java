package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record ExpenseCategoryRow(
        String category,
        int expenseCount,
        BigDecimal total
) {
}