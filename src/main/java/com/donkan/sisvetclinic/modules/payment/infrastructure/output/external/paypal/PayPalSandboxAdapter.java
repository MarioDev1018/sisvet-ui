package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.paypal;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(
        name = "sisvet.payment.paypal.simulated",
        havingValue = "false"
)
public class PayPalSandboxAdapter extends AbstractApprovalPaymentGateway {

    private final RestClient client;
    private final String clientId;
    private final String clientSecret;
    private final String returnUrl;
    private final String cancelUrl;
    private final BigDecimal penPerUsd;

    public PayPalSandboxAdapter(
            @Value("${sisvet.payment.paypal.base-url:https://api-m.sandbox.paypal.com}") String baseUrl,
            @Value("${sisvet.payment.paypal.client-id:}") String clientId,
            @Value("${sisvet.payment.paypal.client-secret:}") String clientSecret,
            @Value("${sisvet.payment.paypal.return-url:http://localhost:8080/panel.html}") String returnUrl,
            @Value("${sisvet.payment.paypal.cancel-url:http://localhost:8080/panel.html}") String cancelUrl,
            @Value("${sisvet.payment.paypal.pen-per-usd:0}") BigDecimal penPerUsd) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
        this.penPerUsd = penPerUsd;
    }

    @Override
    public String provider() {
        return "PAYPAL";
    }

    @Override
    protected GatewayOrder doCreateOrder(PaymentOrder order) {

        BigDecimal chargedAmount = calculateChargeAmount(order);
        String currency = "USD";

        String token = getAccessToken();

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(
                        Map.of(
                                "reference_id", order.id(),
                                "custom_id", order.operationReference(),
                                "description", "SISVET-CLINIC " + order.operationReference(),
                                "amount", Map.of(
                                        "currency_code", currency,
                                        "value", chargedAmount.toPlainString()
                                )
                        )
                ),
                "payment_source", Map.of(
                        "paypal", Map.of(
                                "experience_context", Map.of(
                                        "return_url", returnUrl,
                                        "cancel_url", cancelUrl,
                                        "user_action", "PAY_NOW"
                                )
                        )
                )
        );

        PayPalOrderResponse response = client.post()
                .uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("PayPal-Request-Id", order.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(PayPalOrderResponse.class);

        if (response == null || response.id() == null || response.id().isBlank()) {
            throw new IllegalStateException("PayPal no devolvió el identificador de la orden.");
        }

        String approvalUrl = response.links() == null
                ? null
                : response.links().stream()
                        .filter(link ->
                                "approve".equalsIgnoreCase(link.rel())
                                || "payer-action".equalsIgnoreCase(link.rel())
                        )
                        .map(PayPalLink::href)
                        .findFirst()
                        .orElse(null);

        if (approvalUrl == null || approvalUrl.isBlank()) {
            throw new IllegalStateException("PayPal no devolvió el enlace de aprobación.");
        }

        return new GatewayOrder(
                response.id(),
                approvalUrl,
                chargedAmount,
                currency
        );
    }

    @Override
    protected GatewayVerification doVerifyPayment(PaymentOrder order) {

        try {

            String token = getAccessToken();

            PayPalOrderResponse current = client.get()
                    .uri("/v2/checkout/orders/{id}", order.providerOrderId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(PayPalOrderResponse.class);

            if (current == null) {
                return unknown("PayPal no devolvió la orden.");
            }

            if ("COMPLETED".equalsIgnoreCase(current.status())) {
                return checkCaptures(current, order);
            }

            if ("CREATED".equalsIgnoreCase(current.status())
                    || "PAYER_ACTION_REQUIRED".equalsIgnoreCase(current.status())) {

                return new GatewayVerification(
                        "AWAITING_APPROVAL",
                        null,
                        "El comprador todavía no aprobó el pago."
                );
            }

            if (!"APPROVED".equalsIgnoreCase(current.status())) {
                return unknown("Estado externo no concluyente: " + current.status());
            }

            PayPalOrderResponse captured = client.post()
                    .uri("/v2/checkout/orders/{id}/capture", order.providerOrderId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("PayPal-Request-Id", order.id() + "-capture")
                    .header("Prefer", "return=representation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(PayPalOrderResponse.class);

            if (captured == null) {
                return unknown("PayPal no devolvió el resultado de captura.");
            }

            return checkCaptures(captured, order);

        } catch (RestClientException exception) {

            return unknown(
                    "La comunicación se interrumpió. No se puede afirmar que el pago haya fallado."
            );
        }
    }

    private GatewayVerification checkCaptures(
            PayPalOrderResponse response,
            PaymentOrder order) {

        List<PayPalCapture> captures = response.purchase_units() == null
                ? List.of()
                : response.purchase_units().stream()
                        .filter(unit ->
                                unit.payments() != null
                                && unit.payments().captures() != null
                        )
                        .flatMap(unit -> unit.payments().captures().stream())
                        .toList();

        if (captures.isEmpty()) {
            return unknown("No se encontraron capturas verificables.");
        }

        BigDecimal totalCaptured = BigDecimal.ZERO;
        String captureId = null;

        for (PayPalCapture capture : captures) {

            if ("DECLINED".equalsIgnoreCase(capture.status())
                    || "FAILED".equalsIgnoreCase(capture.status())) {

                return new GatewayVerification(
                        "FAILED",
                        null,
                        "PayPal informó una captura fallida."
                );
            }

            if (!"COMPLETED".equalsIgnoreCase(capture.status())) {

                return unknown(
                        "La captura todavía no está completada: " + capture.status()
                );
            }

            if (capture.amount() == null
                    || !order.chargedCurrency().equalsIgnoreCase(
                            capture.amount().currency_code()
                    )) {

                return unknown("La moneda capturada no coincide con la solicitud.");
            }

            try {

                totalCaptured = totalCaptured.add(
                        new BigDecimal(capture.amount().value())
                );

            } catch (RuntimeException exception) {
                return unknown("El importe de captura no es válido.");
            }

            if (captureId == null) {
                captureId = capture.id();
            }
        }

        if (!"COMPLETED".equalsIgnoreCase(response.status())) {
            return unknown("La orden todavía no tiene estado COMPLETED.");
        }

        if (totalCaptured.compareTo(order.chargedAmount()) != 0) {

            return unknown(
                    "El importe capturado no coincide con el importe registrado."
            );
        }

        if (captureId == null || captureId.isBlank()) {
            return unknown("PayPal no devolvió una referencia de captura.");
        }

        return new GatewayVerification(
                "COMPLETED",
                captureId,
                "Pago capturado y verificado mediante PayPal Sandbox."
        );
    }

    private BigDecimal calculateChargeAmount(PaymentOrder order) {

        if ("USD".equalsIgnoreCase(order.originalCurrency())) {

            return order.originalAmount().setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        if ("PEN".equalsIgnoreCase(order.originalCurrency())) {

            if (penPerUsd == null || penPerUsd.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalStateException(
                        "Debe configurarse un tipo de cambio de prueba PEN/USD."
                );
            }

            BigDecimal converted = order.originalAmount().divide(
                    penPerUsd,
                    2,
                    RoundingMode.HALF_UP
            );

            if (converted.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Importe convertido inválido.");
            }

            return converted;
        }

        throw new IllegalArgumentException(
                "Moneda no configurada para PayPal: " + order.originalCurrency()
        );
    }

    private String getAccessToken() {

        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {

            throw new IllegalStateException(
                    "Faltan credenciales de PayPal Sandbox."
            );
        }

        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        PayPalTokenResponse response = client.post()
                .uri("/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(PayPalTokenResponse.class);

        if (response == null || response.access_token() == null) {
            throw new IllegalStateException("PayPal no devolvió un token OAuth2.");
        }

        return response.access_token();
    }

    private GatewayVerification unknown(String message) {

        return new GatewayVerification(
                "UNKNOWN",
                null,
                message
        );
    }

    private record PayPalTokenResponse(String access_token) { }

    private record PayPalLink(String href, String rel) { }

    private record PayPalMoney(String currency_code, String value) { }

    private record PayPalCapture(
            String id,
            String status,
            PayPalMoney amount
    ) { }

    private record PayPalPaymentCollection(
            List<PayPalCapture> captures
    ) { }

    private record PayPalPurchaseUnit(
            PayPalPaymentCollection payments
    ) { }

    private record PayPalOrderResponse(
            String id,
            String status,
            List<PayPalLink> links,
            List<PayPalPurchaseUnit> purchase_units
    ) { }
}