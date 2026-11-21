package com.donkan.sisvetclinic.modules.finance.service.dto;

public record LowStockProductRow(
        Long productId,
        String productCode,
        String productName,
        String category,
        int stock
) {
}