package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

import java.math.BigDecimal;

public record CheckoutResult(
        String checkoutId,
        String status,
        Long saleId,
        String paymentId,
        String paymentStatus,
        BigDecimal totalAmount,
        String qrBase64,
        String approvalUrl,
        String nextAction,
        String message
) {
}