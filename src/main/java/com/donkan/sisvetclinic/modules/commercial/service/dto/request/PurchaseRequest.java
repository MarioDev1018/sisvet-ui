package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseRequest(
        Long supplierId,
        Long paymentMethodId,
        String operationKey,
        List<Item> items
) {
    public record Item(
            Long productId,
            Long presentationId,
            int quantity,
            BigDecimal unitCost
    ) {
    }
}