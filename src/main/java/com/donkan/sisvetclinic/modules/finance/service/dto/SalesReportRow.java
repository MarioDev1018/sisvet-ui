package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalesReportRow(
        Long saleId,
        LocalDateTime saleDatetime,
        String seller,
        String customer,
        String paymentMethod,
        BigDecimal total
) {
}