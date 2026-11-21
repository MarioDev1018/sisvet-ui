package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.external;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentRequest;
import com.donkan.sisvetclinic.modules.payment.service.dto.response.PaymentResult;
import com.donkan.sisvetclinic.modules.payment.service.port.in.PaymentUseCase;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class PaymentModuleAdapter implements PaymentProcessingPort {

    private final PaymentUseCase payments;

    public PaymentModuleAdapter(PaymentUseCase payments) {
        this.payments = payments;
    }

    @Override
    public boolean supports(String provider) {
        return payments.supportsProvider(provider);
    }

    @Override
    public PaymentReceipt initiate(
            String operationReference,
            BigDecimal amount,
            String currency,
            String provider) {

        return convert(payments.initiatePayment(
                new PaymentRequest(operationReference, provider, amount, currency)
        ));
    }

    @Override
    public PaymentReceipt submitCredentials(
            String paymentId, CheckoutCredentials credentials) {

        PaymentCredentials request = new PaymentCredentials(
                credentials.phoneNumber(),
                credentials.otp(),
                credentials.payerEmail()
        );

        return convert(payments.submitCredentials(paymentId, request));
    }

    @Override
    public PaymentReceipt verify(String paymentId) {
        return convert(payments.verifyPayment(paymentId));
    }

    @Override
    public PaymentReceipt find(String paymentId) {
        return convert(payments.findPayment(paymentId));
    }

    private PaymentReceipt convert(PaymentResult result) {

        return new PaymentReceipt(
                result.paymentId(),
                result.operationReference(),
                result.provider(),
                result.originalAmount(),
                result.originalCurrency(),
                result.chargedAmount(),
                result.chargedCurrency(),
                result.providerOrderId(),
                result.captureReference(),
                result.approvalUrl(),
                result.qrBase64(),
                result.status(),
                result.message(),
                result.nextAction()
        );
    }
}