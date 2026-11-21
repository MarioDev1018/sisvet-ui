package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

import java.util.List;

public record SaleRequest(
        Long customerId,
        Long paymentMethodId,
        String operationKey,
        List<Item> items
) {
    public record Item(
            Long productId,
            Long presentationId,
            int quantity,
            Integer requestedBaseQuantity
    ) {
    }
}