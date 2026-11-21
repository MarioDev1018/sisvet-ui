package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseRequest(
        String name,
        String description,
        BigDecimal amount,
        String category,
        LocalDateTime expenseDatetime
) {
}