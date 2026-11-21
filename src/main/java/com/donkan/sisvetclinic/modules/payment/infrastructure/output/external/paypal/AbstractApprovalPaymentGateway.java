package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.paypal;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentGatewayPort;

import java.math.BigDecimal;

public abstract class AbstractApprovalPaymentGateway implements PaymentGatewayPort {

    @Override
    public final GatewayOrder createOrder(PaymentOrder order) {

        if (order == null || order.originalAmount() == null
                || order.originalAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "La solicitud de pago no es válida."
            );
        }

        GatewayOrder created = doCreateOrder(order);

        if (created == null
                || created.orderId() == null
                || created.orderId().isBlank()
                || created.approvalUrl() == null
                || created.approvalUrl().isBlank()
                || created.chargedAmount() == null
                || created.chargedAmount().signum() <= 0
                || created.chargedCurrency() == null
                || created.chargedCurrency().isBlank()) {

            throw new IllegalStateException(
                    "El proveedor devolvió una orden incompleta."
            );
        }

        return created;
    }

    @Override
    public final GatewayVerification verifyPayment(PaymentOrder order) {

        if (order == null
                || order.providerOrderId() == null
                || order.providerOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "La orden externa no existe."
            );
        }

        GatewayVerification result = doVerifyPayment(order);

        if (result == null
                || result.status() == null
                || result.status().isBlank()) {

            return new GatewayVerification(
                    "UNKNOWN",
                    null,
                    "Resultado externo indeterminado."
            );
        }

        return result;
    }

    protected abstract GatewayOrder doCreateOrder(PaymentOrder order);

    protected abstract GatewayVerification doVerifyPayment(PaymentOrder order);
}