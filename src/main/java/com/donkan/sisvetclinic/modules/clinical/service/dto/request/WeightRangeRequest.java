package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.math.BigDecimal;

public record WeightRangeRequest(
    String species,
    BigDecimal minimum,
    BigDecimal maximum
) {}
