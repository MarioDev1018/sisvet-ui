package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

import java.math.BigDecimal;

public record ProductPresentationRequest(
        String name,
        int quantityInBase,
        BigDecimal salePrice,
        boolean fractionable
) {
}