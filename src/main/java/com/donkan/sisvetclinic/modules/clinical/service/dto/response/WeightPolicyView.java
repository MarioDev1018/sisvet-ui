package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;

public record WeightPolicyView(
    String species,
    String speciesLabel,
    BigDecimal minKg,
    BigDecimal maxKg
) {}
