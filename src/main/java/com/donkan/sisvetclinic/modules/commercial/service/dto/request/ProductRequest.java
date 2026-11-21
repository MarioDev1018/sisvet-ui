package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

import java.math.BigDecimal;

public record ProductRequest(
        String code,
        String name,
        BigDecimal price,
        Long categoryId,
        String baseUnit,
        String presentationName,
        Integer presentationQuantity,
        Boolean fractionable
) {
}