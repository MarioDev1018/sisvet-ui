package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.math.BigDecimal;

public record SupplierPurchasesRow(
        String ruc,
        String supplierName,
        int purchaseCount,
        BigDecimal total
) {
}