package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseReportRow(
        Long expenseId,
        LocalDateTime expenseDatetime,
        String name,
        String description,
        String category,
        BigDecimal amount,
        boolean automatic,
        String registeredBy
) {
}