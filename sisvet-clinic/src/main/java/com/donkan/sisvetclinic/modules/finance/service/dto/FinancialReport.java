package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialReport(
        LocalDate startDate,
        LocalDate endDate,
        long saleCount,
        BigDecimal salesTotal,
        long purchaseCount,
        BigDecimal purchasesTotal,
        long expenseCount,
        BigDecimal expensesTotal,
        BigDecimal totalOutflows,
        BigDecimal balance,
        BigDecimal averageTicket,
        BigDecimal balancePercentage
) {
}