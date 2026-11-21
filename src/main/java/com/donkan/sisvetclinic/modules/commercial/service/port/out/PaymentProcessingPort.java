package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;

import java.math.BigDecimal;

public interface PaymentProcessingPort {

    record PaymentReceipt(
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

    PaymentReceipt initiate(
            String operationReference,
            BigDecimal amount,
            String currency,
            String provider
    );

    PaymentReceipt submitCredentials(String paymentId, CheckoutCredentials credentials);

    PaymentReceipt verify(String paymentId);

    PaymentReceipt find(String paymentId);

    boolean supports(String provider);
}