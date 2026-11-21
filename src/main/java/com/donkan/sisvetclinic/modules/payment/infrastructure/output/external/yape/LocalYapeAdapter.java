package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.yape;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;
import com.donkan.sisvetclinic.modules.payment.service.port.out.ImmediatePaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentGatewayPort;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "sisvet.payment.yape.simulated",
        havingValue = "true",
        matchIfMissing = true
)
public class LocalYapeAdapter implements ImmediatePaymentGatewayPort {

    @Override
    public String provider() {
        return "YAPE";
    }

    @Override
    public ExecutionResult execute(PaymentOrder order, PaymentCredentials credentials) {

        boolean approved = "111111111".equals(credentials.phoneNumber())
                && "123456".equals(credentials.otp());

        String externalId = "SIM-YAPE-" + order.id();

        return new ExecutionResult(
                externalId,
                approved ? "SIM-YAPE-CAP-" + order.id() : null,
                order.originalAmount(),
                order.originalCurrency(),
                approved ? "COMPLETED" : "FAILED",
                approved
                        ? "Pago Yape aprobado en simulación local."
                        : "Pago Yape rechazado por el simulador."
        );
    }

    @Override
    public PaymentGatewayPort.GatewayVerification verifyPayment(PaymentOrder order) {
        return new PaymentGatewayPort.GatewayVerification(
                "UNKNOWN",
                null,
                "La simulación debe resolverse al enviar las credenciales."
        );
    }
}