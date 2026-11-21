package com.donkan.sisvetclinic.modules.payment.service.port.out;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import java.math.BigDecimal;

public interface PaymentGatewayPort {

    record GatewayOrder(
            String orderId,
            String approvalUrl,
            BigDecimal chargedAmount,
            String chargedCurrency
    ) {
    }

    record GatewayVerification(
            String status,
            String captureReference,
            String message
    ) {
    }

    String provider();

    GatewayOrder createOrder(PaymentOrder order);

    GatewayVerification verifyPayment(PaymentOrder order);
}