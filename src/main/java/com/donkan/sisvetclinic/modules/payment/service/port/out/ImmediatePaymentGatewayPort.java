package com.donkan.sisvetclinic.modules.payment.service.port.out;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;

import java.math.BigDecimal;

public interface ImmediatePaymentGatewayPort {

    record ExecutionResult(
            String providerOrderId,
            String captureReference,
            BigDecimal chargedAmount,
            String chargedCurrency,
            String status,
            String message
    ) {
    }

    String provider();

    ExecutionResult execute(PaymentOrder order, PaymentCredentials credentials);

    PaymentGatewayPort.GatewayVerification verifyPayment(PaymentOrder order);
}