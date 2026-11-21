package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.yape;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;
import com.donkan.sisvetclinic.modules.payment.service.port.out.ImmediatePaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentGatewayPort;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(
        name = "sisvet.payment.yape.simulated",
        havingValue = "false"
)
public class MercadoPagoYapeAdapter implements ImmediatePaymentGatewayPort {

    private final RestClient client;
    private final String publicKey;
    private final String accessToken;

    public MercadoPagoYapeAdapter(
            @Value("${sisvet.payment.yape.base-url:https://api.mercadopago.com}") String baseUrl,
            @Value("${sisvet.payment.yape.public-key:}") String publicKey,
            @Value("${sisvet.payment.yape.access-token:}") String accessToken) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        this.publicKey = publicKey;
        this.accessToken = accessToken;
    }

    @Override
    public String provider() {
        return "YAPE";
    }

    @Override
    public ExecutionResult execute(PaymentOrder order, PaymentCredentials credentials) {

        validateConfiguration();

        if (!"PEN".equals(order.originalCurrency())) {
            throw new IllegalArgumentException("Yape requiere importes expresados en PEN.");
        }

        Map<String, Object> tokenBody = Map.of(
                "phoneNumber", credentials.phoneNumber(),
                "otp", credentials.otp(),
                "requestId", order.id()
        );

        YapeToken token;
        try {
            token = client.post()
                    .uri(builder -> builder
                            .path("/platforms/pci/yape/v1/payment")
                            .queryParam("public_key", publicKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(tokenBody)
                    .retrieve()
                    .body(YapeToken.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                return new ExecutionResult(
                        "MP-TOKEN-REJECTED-" + order.id(),
                        null,
                        order.originalAmount(),
                        "PEN",
                        "FAILED",
                        "Mercado Pago rechazó los datos de autorización Yape (teléfono u OTP inválido)."
                );
            }
            throw new IllegalStateException("Error al solicitar el token Yape en Mercado Pago.", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("No se pudo conectar con Mercado Pago para generar el token Yape.", exception);
        }

        if (token == null || token.id() == null || token.id().isBlank()) {
            throw new IllegalStateException("Mercado Pago no devolvió el token Yape.");
        }

        String amount = order.originalAmount().toPlainString();

        Map<String, Object> body = Map.of(
                "type", "online",
                "external_reference", order.operationReference(),
                "processing_mode", "automatic",
                "total_amount", amount,
                "payer", Map.of(
                        "email", credentials.payerEmail(),
                        "entity_type", "individual",
                        "phone", Map.of(
                                "area_code", "51",
                                "number", credentials.phoneNumber()
                        )
                ),
                "transactions", Map.of(
                        "payments", List.of(
                                Map.of(
                                        "amount", amount,
                                        "payment_method", Map.of(
                                                "id", "yape",
                                                "type", "debit_card",
                                                "token", token.id()
                                        )
                                )
                        )
                )
        );

        MercadoPagoOrder response;
        try {
            response = client.post()
                    .uri("/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("X-Idempotency-Key", order.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MercadoPagoOrder.class);
        } catch (RestClientException exception) {
            return unknown(null, order, "No se pudo confirmar la orden Yape con Mercado Pago.");
        }

        return interpret(response, order);
    }

    @Override
    public PaymentGatewayPort.GatewayVerification verifyPayment(PaymentOrder order) {

        validateConfiguration();

        if (order.providerOrderId() == null) {
            return new PaymentGatewayPort.GatewayVerification(
                    "UNKNOWN", null, "No existe una orden externa verificable."
            );
        }

        MercadoPagoOrder response;
        try {
            response = client.get()
                    .uri("/v1/orders/{id}", order.providerOrderId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(MercadoPagoOrder.class);
        } catch (RestClientException exception) {
            return new PaymentGatewayPort.GatewayVerification(
                    "UNKNOWN", null, "No se pudo consultar la orden en Mercado Pago."
            );
        }

        ExecutionResult result = interpret(response, order);

        return new PaymentGatewayPort.GatewayVerification(
                result.status(),
                result.captureReference(),
                result.message()
        );
    }

    private ExecutionResult interpret(MercadoPagoOrder response, PaymentOrder order) {

        if (response == null || response.id() == null || response.id().isBlank()) {
            return unknown(null, order, "Mercado Pago no devolvió una orden válida.");
        }

        if (!order.operationReference().equals(response.external_reference())
                || !"PEN".equalsIgnoreCase(response.currency())
                || !sameAmount(response.total_amount(), order.originalAmount())) {

            return unknown(response.id(), order,
                    "La orden externa no coincide con la operación comercial.");
        }

        if ("failed".equalsIgnoreCase(response.status())
                || "cancelled".equalsIgnoreCase(response.status())) {

            return new ExecutionResult(
                    response.id(), null, order.originalAmount(), "PEN",
                    "FAILED", "Mercado Pago informó que el pago no se completó."
            );
        }

        if (!"processed".equalsIgnoreCase(response.status())
                || !"accredited".equalsIgnoreCase(response.status_detail())
                || !sameAmount(response.total_paid_amount(), order.originalAmount())) {

            return unknown(response.id(), order,
                    "La orden todavía no acredita el importe completo.");
        }

        List<MercadoPagoTransaction> transactions = response.transactions() == null
                || response.transactions().payments() == null
                ? List.of()
                : response.transactions().payments();

        if (transactions.size() != 1) {
            return unknown(response.id(), order,
                    "La orden no contiene una única transacción Yape verificable.");
        }

        MercadoPagoTransaction transaction = transactions.getFirst();

        if (transaction.id() == null || transaction.id().isBlank()
                || !"processed".equalsIgnoreCase(transaction.status())
                || !"accredited".equalsIgnoreCase(transaction.status_detail())
                || !sameAmount(transaction.paid_amount(), order.originalAmount())
                || transaction.payment_method() == null
                || !"yape".equalsIgnoreCase(transaction.payment_method().id())) {

            return unknown(response.id(), order,
                    "El detalle del pago no acredita la transacción esperada.");
        }

        return new ExecutionResult(
                response.id(),
                transaction.id(),
                order.originalAmount(),
                "PEN",
                "COMPLETED",
                "Pago Yape procesado y acreditado por Mercado Pago."
        );
    }

    private ExecutionResult unknown(String externalId, PaymentOrder order, String message) {

        return new ExecutionResult(
                externalId, null, order.originalAmount(), "PEN", "UNKNOWN", message
        );
    }

    private boolean sameAmount(String value, BigDecimal expected) {

        if (value == null || expected == null) return false;

        try {
            return new BigDecimal(value).compareTo(expected) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void validateConfiguration() {

        if (publicKey == null || publicKey.isBlank()
                || accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Faltan credenciales de Mercado Pago.");
        }
    }

    private record YapeToken(String id) { }

    private record MercadoPagoMethod(String id) { }

    private record MercadoPagoTransaction(
            String id,
            String status,
            String status_detail,
            String paid_amount,
            MercadoPagoMethod payment_method
    ) { }

    private record MercadoPagoTransactions(
            List<MercadoPagoTransaction> payments
    ) { }

    private record MercadoPagoOrder(
            String id,
            String status,
            String status_detail,
            String external_reference,
            String currency,
            String total_amount,
            String total_paid_amount,
            MercadoPagoTransactions transactions
    ) { }
}