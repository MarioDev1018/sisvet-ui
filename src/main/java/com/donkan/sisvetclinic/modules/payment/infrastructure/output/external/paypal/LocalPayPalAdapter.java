package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.paypal;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentRepositoryPort;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
@ConditionalOnProperty(
        name = "sisvet.payment.paypal.simulated",
        havingValue = "true",
        matchIfMissing = true
)
public class LocalPayPalAdapter extends AbstractApprovalPaymentGateway {

    private final PaymentRepositoryPort payments;
    private final String publicBaseUrl;

    public LocalPayPalAdapter(
            PaymentRepositoryPort payments,
            @Value("${sisvet.payment.public-base-url:http://localhost:8080}") String publicBaseUrl) {

        this.payments = payments;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public String provider() {
        return "PAYPAL";
    }

    @Override
    protected GatewayOrder doCreateOrder(PaymentOrder order) {

        String id = "SIM-PP-" + order.id().toUpperCase(Locale.ROOT);

        return new GatewayOrder(
                id,
                resolveBaseUrl() + "/simulacion/paypal-checkout/" + id,
                order.originalAmount(),
                order.originalCurrency()
        );
    }

    private String resolveBaseUrl() {
        if (!"http://localhost:8080".equalsIgnoreCase(publicBaseUrl)) {
            return publicBaseUrl;
        }
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .build()
                    .toUriString()
                    .replaceAll("/+$", "");
        } catch (IllegalStateException ignored) {
            return publicBaseUrl;
        }
    }

    @Override
    protected GatewayVerification doVerifyPayment(PaymentOrder order) {

        String decision = payments.simulationDecision(
                order.providerOrderId()
        );

        if (decision == null) {

            return new GatewayVerification(
                    "UNKNOWN",
                    null,
                    "No se encontró la orden simulada."
            );
        }

        return switch (decision) {

            case "APPROVED" -> new GatewayVerification(
                    "COMPLETED",
                    "SIM-CAP-" + order.providerOrderId(),
                    "Pago simulado confirmado."
            );

            case "REJECTED" -> new GatewayVerification(
                    "FAILED",
                    null,
                    "Pago simulado rechazado."
            );

            default -> new GatewayVerification(
                    "AWAITING_APPROVAL",
                    null,
                    "Esperando aprobación del cliente."
            );
        };
    }

    public void approveOrder(String providerOrderId) {
        payments.decideSimulation(providerOrderId, "APPROVED");
    }

    public void rejectOrder(String providerOrderId) {
        payments.decideSimulation(providerOrderId, "REJECTED");
    }
}