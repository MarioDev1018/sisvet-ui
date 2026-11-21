package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClinicalRevenueReportRow(
        Long saleId,
        LocalDateTime saleDatetime,
        String petName,
        String ownerName,
        String conceptSummary,
        String paymentMethod,
        BigDecimal total
) {}
