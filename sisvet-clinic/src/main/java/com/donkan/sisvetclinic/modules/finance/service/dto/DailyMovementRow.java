package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMovementRow(
        LocalDate date,
        BigDecimal salesTotal,
        BigDecimal purchasesTotal,
        BigDecimal expensesTotal,
        BigDecimal balance
) {
}