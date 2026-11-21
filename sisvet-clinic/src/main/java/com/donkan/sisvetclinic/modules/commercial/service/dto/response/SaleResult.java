package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleResult(
        Long saleId,
        BigDecimal totalAmount,
        LocalDateTime saleDatetime,
        String status
) {
}