package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;

public record WeightCheckResult(
    boolean configured,
    boolean warning,
    String species,
    String speciesLabel,
    BigDecimal minKg,
    BigDecimal maxKg,
    BigDecimal lastWeightKg,
    String message
) {}
