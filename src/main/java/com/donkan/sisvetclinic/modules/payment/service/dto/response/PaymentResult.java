package com.donkan.sisvetclinic.modules.payment.service.dto.response;

import java.math.BigDecimal;

public record PaymentResult(
        String paymentId,
        String operationReference,
        String provider,
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal chargedAmount,
        String chargedCurrency,
        String providerOrderId,
        String captureReference,
        String approvalUrl,
        String qrBase64,
        String status,
        String message,
        String nextAction
) {
}