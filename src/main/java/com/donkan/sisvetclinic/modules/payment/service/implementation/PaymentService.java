package com.donkan.sisvetclinic.modules.payment.service.implementation;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentRequest;
import com.donkan.sisvetclinic.modules.payment.service.dto.response.PaymentResult;
import com.donkan.sisvetclinic.modules.payment.service.port.in.PaymentUseCase;
import com.donkan.sisvetclinic.modules.payment.service.port.out.ImmediatePaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentRepositoryPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.QrGeneratorPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentService implements PaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepositoryPort payments;
    private final QrGeneratorPort qrGenerator;
    private final List<PaymentGatewayPort> approvalGateways;
    private final List<ImmediatePaymentGatewayPort> immediateGateways;

    public PaymentService(
            PaymentRepositoryPort payments,
            QrGeneratorPort qrGenerator,
            List<PaymentGatewayPort> approvalGateways,
            List<ImmediatePaymentGatewayPort> immediateGateways) {

        this.payments = payments;
        this.qrGenerator = qrGenerator;
        this.approvalGateways = approvalGateways;
        this.immediateGateways = immediateGateways;
    }

    @Override
    public boolean supportsProvider(String provider) {

        if (provider == null || provider.isBlank()) return false;

        return approvalGateways.stream()
                .anyMatch(g -> g.provider().equalsIgnoreCase(provider))
                || immediateGateways.stream()
                .anyMatch(g -> g.provider().equalsIgnoreCase(provider));
    }

    @Override
    public PaymentResult initiatePayment(PaymentRequest request) {

        validateRequest(request);

        String provider = normalize(request.provider());
        String currency = normalize(request.currency());

        if (!supportsProvider(provider)) {
            throw new IllegalArgumentException("Pasarela no disponible: " + provider);
        }

        PaymentOrder order = payments.findByOperationReference(
                request.operationReference()
        ).orElse(null);

        if (order == null) {

            PaymentOrder candidate = PaymentOrder.pending(
                    request.operationReference(),
                    provider,
                    request.amount(),
                    currency
            );

            payments.insertIfAbsent(candidate);

            order = payments.findByOperationReference(
                    request.operationReference()
            ).orElseThrow();
        }

        if (!order.provider().equals(provider)
                || !order.originalCurrency().equals(currency)
                || order.originalAmount().compareTo(request.amount()) != 0) {

            throw new IllegalStateException(
                    "Esta referencia ya pertenece a un pago diferente."
            );
        }

        if (immediateGateway(provider) != null) {

            return toResult(
                    order,
                    "Introduce los datos de autorización para procesar el pago."
            );
        }

        if (order.providerOrderId() != null
                || !order.state().canStartCreation()) {

            return toResult(order, "La orden ya existe.");
        }

        if (!payments.tryStartCreation(order.id())) {

            return toResult(
                    findOrder(order.id()),
                    "La orden se está procesando."
            );
        }

        PaymentOrder creating = findOrder(order.id());

        try {

            PaymentGatewayPort.GatewayOrder external =
                    approvalGateway(provider).createOrder(creating);

            if (external == null || external.orderId() == null
                    || external.orderId().isBlank()
                    || external.approvalUrl() == null
                    || external.approvalUrl().isBlank()
                    || external.chargedAmount() == null
                    || external.chargedAmount().signum() <= 0
                    || external.chargedCurrency() == null
                    || external.chargedCurrency().isBlank()) {
                throw new IllegalStateException("La pasarela devolvió una orden incompleta.");
            }

            PaymentOrder created = creating.orderCreated(
                    external.orderId(),
                    external.approvalUrl(),
                    external.chargedAmount(),
                    external.chargedCurrency()
            );

            payments.update(created, "CREATING");

            return toResult(created, "Esperando aprobación del cliente.");

        } catch (RuntimeException exception) {

            PaymentOrder unknown = creating.creationUnknown();
            payments.update(unknown, "CREATING");

            return toResult(
                    unknown,
                    "No se confirmó la creación de la orden. Requiere verificación."
            );
        }
    }

    @Override
    public PaymentResult submitCredentials(
            String paymentId, PaymentCredentials credentials) {

        if (credentials == null) {
            throw new IllegalArgumentException("Faltan los datos de autorización.");
        }
        
        PaymentOrder order = findOrder(paymentId);
        ImmediatePaymentGatewayPort gateway = immediateGateway(order.provider());

        if (gateway == null) {
            throw new IllegalStateException(
                    "Esta pasarela no admite autorización inmediata."
            );
        }

        if (order.providerOrderId() != null
                || (!"PENDING".equals(order.statusCode())
                && !"CREATING".equals(order.statusCode()))) {

            return toResult(
                    order,
                    "La operación ya fue enviada o necesita recuperación. No se repetirá el cobro."
            );
        }

        if (!payments.tryStartCreation(paymentId)) {

            return toResult(
                    findOrder(paymentId),
                    "Otro proceso está enviando la orden."
            );
        }

        PaymentOrder creating = findOrder(paymentId);

        try {

            ImmediatePaymentGatewayPort.ExecutionResult external = gateway.execute(creating, credentials);

            if (external == null) {
                throw new IllegalStateException("No existe respuesta verificable.");
            }

            String resultStatus = external.status();

            boolean validAmount = external.chargedAmount() != null
                    && creating.originalAmount().compareTo(external.chargedAmount()) == 0
                    && creating.originalCurrency().equalsIgnoreCase(
                            external.chargedCurrency() == null ? "" : external.chargedCurrency()
                    );

            if (!validAmount || external.providerOrderId() == null
                    || external.providerOrderId().isBlank()) {
                resultStatus = "UNKNOWN";
            }

            if ("COMPLETED".equals(resultStatus)
                    && (external.captureReference() == null
                    || external.captureReference().isBlank())) {
                resultStatus = "UNKNOWN";
            }

            PaymentOrder updated = creating.immediateResult(
                    external.providerOrderId(),
                    "COMPLETED".equals(resultStatus) ? external.captureReference() : null,
                    external.chargedAmount(),
                    external.chargedCurrency(),
                    resultStatus
            );

            payments.update(updated, "CREATING");

            return toResult(updated, external.message());

        } catch (RuntimeException exception) {

            log.error("Error al procesar pago inmediato {} con adaptador {} (estado={})",
                    paymentId, gateway.getClass().getSimpleName(), creating.statusCode(), exception);

            PaymentOrder unknown = creating.creationUnknown();
            payments.update(unknown, "CREATING");

            return toResult(
                    unknown,
                    "El procesamiento del pago falló. Revisa el registro del servidor."
            );
        }
    }

    @Override
    public PaymentResult verifyPayment(String paymentId) {

        PaymentOrder order = findOrder(paymentId);

        if (order.state().isFinal()) {
            return toResult(order, "La operación ya tiene un resultado definitivo.");
        }

        if (order.providerOrderId() == null
                || !order.state().canStartVerification()) {

            return toResult(order, "Todavía no existe una orden externa verificable.");
        }

        if (!payments.tryStartVerification(paymentId)) {

            return toResult(
                    findOrder(paymentId),
                    "La verificación está en curso."
            );
        }

        PaymentOrder verifying = findOrder(paymentId);
        PaymentGatewayPort.GatewayVerification external;

        try {

            ImmediatePaymentGatewayPort immediate = immediateGateway(verifying.provider());

            external = immediate != null
                    ? immediate.verifyPayment(verifying)
                    : approvalGateway(verifying.provider()).verifyPayment(verifying);

        } catch (RuntimeException exception) {

            external = new PaymentGatewayPort.GatewayVerification(
                    "UNKNOWN",
                    null,
                    "No se pudo determinar el resultado del proveedor."
            );
        }

        if (external == null) {
            external = new PaymentGatewayPort.GatewayVerification(
                    "UNKNOWN", null, "Respuesta externa vacía."
            );
        }

        PaymentOrder updated = verifying.verificationResult(
                external.status(),
                external.captureReference()
        );

        payments.update(updated, "VERIFYING");

        return toResult(updated, external.message());
    }

    @Override
    public PaymentResult findPayment(String paymentId) {
        return toResult(findOrder(paymentId), "Estado almacenado.");
    }

    private PaymentGatewayPort approvalGateway(String provider) {

        return approvalGateways.stream()
                .filter(g -> g.provider().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una pasarela de aprobación para " + provider
                ));
    }

    private ImmediatePaymentGatewayPort immediateGateway(String provider) {

        return immediateGateways.stream()
                .filter(g -> g.provider().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);
    }

    private PaymentOrder findOrder(String paymentId) {

        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("El identificador de pago es obligatorio.");
        }

        return payments.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("El pago no existe."));
    }

    private void validateRequest(PaymentRequest request) {

        if (request == null || request.operationReference() == null
                || request.operationReference().isBlank()
                || request.operationReference().length() > 100
                || request.amount() == null || request.amount().signum() <= 0
                || request.amount().scale() > 2
                || !normalize(request.currency()).matches("[A-Z]{3}")
                || normalize(request.provider()).isBlank()) {

            throw new IllegalArgumentException("La solicitud de pago no es válida.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private PaymentResult toResult(PaymentOrder order, String message) {

        String qr = null;

        if (order.approvalUrl() != null && !order.state().isFinal()) {
            try {
                qr = qrGenerator.generateBase64(order.approvalUrl(), 280, 280);
            } catch (RuntimeException ignored) {
                // Un fallo del QR no cambia el resultado financiero.
            }
        }

        String nextAction = "WAIT";

        if (order.state().isFinal()) {
            nextAction = "NONE";
        } else if ("UNKNOWN".equals(order.statusCode())
                && order.providerOrderId() == null) {
            nextAction = "RECOVER";
        } else if (immediateGateway(order.provider()) != null
                && "PENDING".equals(order.statusCode())) {
            nextAction = "COLLECT_CREDENTIALS";
        } else if (qr != null) {
            nextAction = "DISPLAY_QR";
        } else if (order.approvalUrl() != null) {
            nextAction = "OPEN_URL";
        }

        return new PaymentResult(
                order.id(),
                order.operationReference(),
                order.provider(),
                order.originalAmount(),
                order.originalCurrency(),
                order.chargedAmount(),
                order.chargedCurrency(),
                order.providerOrderId(),
                order.captureReference(),
                order.approvalUrl(),
                qr,
                order.statusCode(),
                message,
                nextAction
        );
    }
}