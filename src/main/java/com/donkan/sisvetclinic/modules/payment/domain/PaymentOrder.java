package com.donkan.sisvetclinic.modules.payment.domain;

import com.donkan.sisvetclinic.modules.payment.domain.state.PaymentState;
import com.donkan.sisvetclinic.modules.payment.domain.state.PendingState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record PaymentOrder(
        String id,
        String operationReference,
        String provider,
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal chargedAmount,
        String chargedCurrency,
        String providerOrderId,
        String captureReference,
        String approvalUrl,
        PaymentState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PaymentOrder pending(String operationReference, String provider,
                                       BigDecimal amount, String currency) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        return new PaymentOrder(
                UUID.randomUUID().toString(),
                operationReference,
                provider,
                amount,
                currency,
                null,
                null,
                null,
                null,
                null,
                new PendingState(),
                now,
                now
        );
    }

    public String statusCode() {
        return state.code();
    }

    public PaymentOrder startCreation() {
        return copy(chargedAmount, chargedCurrency, providerOrderId,
                captureReference, approvalUrl, state.startCreation());
    }

    public PaymentOrder orderCreated(String orderId, String url,
                                     BigDecimal amount, String currency) {

        return copy(amount, currency, orderId,
                captureReference, url, state.orderCreated());
    }

    public PaymentOrder creationUnknown() {
        return copy(chargedAmount, chargedCurrency, providerOrderId,
                captureReference, approvalUrl, state.creationUnknown());
    }

    public PaymentOrder startVerification() {
        return copy(chargedAmount, chargedCurrency, providerOrderId,
                captureReference, approvalUrl, state.startVerification());
    }

    public PaymentOrder verificationResult(String status, String captureId) {

        return copy(
                chargedAmount,
                chargedCurrency,
                providerOrderId,
                captureId == null ? captureReference : captureId,
                approvalUrl,
                state.resolveVerification(status)
        );
    }

    private PaymentOrder copy(BigDecimal charged, String currency,
                              String externalId, String captureId,
                              String url, PaymentState newState) {

        return new PaymentOrder(
                id,
                operationReference,
                provider,
                originalAmount,
                originalCurrency,
                charged,
                currency,
                externalId,
                captureId,
                url,
                newState,
                createdAt,
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }
    
    public PaymentOrder immediateResult(
            String orderId,
            String paymentReference,
            java.math.BigDecimal amount,
            String currency,
            String result) {

        return copy(
                amount,
                currency,
                orderId,
                paymentReference,
                null,
                state.finishImmediate(result)
        );
    }
}