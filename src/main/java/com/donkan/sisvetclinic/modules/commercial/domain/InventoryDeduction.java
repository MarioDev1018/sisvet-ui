package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public record InventoryDeduction(
        boolean successful,
        BigDecimal averageUnitCost
) {
}