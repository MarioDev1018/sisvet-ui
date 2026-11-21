package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public record CheckoutOperation(
        String operationKey,
        Long userId,
        Long customerId,
        Long paymentMethodId,
        String provider,
        BigDecimal totalAmount,
        String fingerprint,
        String status,
        String paymentId,
        Long saleId
) {
}