package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public record CheckoutItem(
        Long productId,
        Long presentationId,
        int quantity,
        Integer requestedBaseQuantity,
        int baseQuantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}