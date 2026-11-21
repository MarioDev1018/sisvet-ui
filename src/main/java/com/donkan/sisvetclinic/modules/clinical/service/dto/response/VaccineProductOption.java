package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;

public record VaccineProductOption(
    long productId,
    String code,
    String name,
    String categoryName,
    String baseUnit,
    int availableStock,
    BigDecimal unitPricePerBase,
    BigDecimal defaultPresentationPrice,
    int defaultPresentationQuantity
) {}
