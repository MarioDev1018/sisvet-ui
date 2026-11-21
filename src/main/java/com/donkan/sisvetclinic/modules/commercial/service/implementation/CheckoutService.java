package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutItem;
import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutOperation;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.CheckoutResult;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.CheckoutUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SaleUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CheckoutRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort.PaymentReceipt;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class CheckoutService implements CheckoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CheckoutRepositoryPort checkouts;
    private final SaleUseCase sales;
    private final PaymentProcessingPort payments;
    private final CheckoutFinalizationService finalization;

    @Value("${sisvet.payment.yape.simulated:false}")
    private boolean simulatedYape;

    public CheckoutService(CheckoutRepositoryPort checkouts, SaleUseCase sales,
                           PaymentProcessingPort payments, CheckoutFinalizationService finalization) {
        this.checkouts = checkouts;
        this.sales = sales;
        this.payments = payments;
        this.finalization = finalization;
    }

    @Override
    public CheckoutResult start(Long userId, SaleRequest request) {
        if (userId == null || userId <= 0 || request == null || request.paymentMethodId() == null
                || request.operationKey() == null || request.operationKey().isBlank()) {
            throw new IllegalArgumentException("La solicitud de venta es inválida.");
        }

        CheckoutOperation previous = checkouts.findByKey(request.operationKey()).orElse(null);
        if (previous != null) {
            validateOwner(userId, previous);
            if (isTerminal(previous.status())) return buildResult(previous, null, "Operación ya finalizada.");
        }

        CheckoutRepositoryPort.PaymentRoute route = checkouts.findPaymentRoute(request.paymentMethodId());

        if ("MANUAL".equalsIgnoreCase(route.processingType())) {
            if (previous != null) {
                throw new IllegalStateException("La clave ya corresponde a un checkout electrónico.");
            }
            SaleResult sale = sales.registerSale(userId, request);
            return new CheckoutResult(request.operationKey(), "COMPLETED", sale.saleId(), null, null,
                    sale.totalAmount(), null, null, "NONE", "Venta registrada mediante cobro manual.");
        }

        if (!"EXTERNAL".equalsIgnoreCase(route.processingType()) || route.provider() == null
                || !payments.supports(route.provider())) {
            throw new IllegalStateException("La pasarela del método seleccionado no está disponible.");
        }

        CheckoutOperation checkout = checkouts.reserve(userId, request, route.provider());
        validateOwner(userId, checkout);

        if (isTerminal(checkout.status())) return buildResult(checkout, null, "Operación ya finalizada.");
        if ("RECOVERY_REQUIRED".equals(checkout.status())) return find(userId, checkout.operationKey());

        if (checkout.paymentId() != null) {
            PaymentReceipt existingReceipt = payments.find(checkout.paymentId());
            if (!matchesReceipt(checkout, existingReceipt)) {
                checkouts.markRecoveryRequired(checkout.operationKey());
                return buildResult(getCheckout(checkout.operationKey()), null,
                        "La respuesta de Payment no coincide con el checkout.");
            }
            if ("COMPLETED".equals(existingReceipt.status())) {
                return finish(userId, checkout.operationKey(), existingReceipt);
            }
            if ("FAILED".equals(existingReceipt.status())) {
                checkouts.cancelFailedCheckout(checkout.operationKey(), existingReceipt.paymentId());
                return buildResult(getCheckout(checkout.operationKey()), existingReceipt, "El pago fue rechazado.");
            }
            if ("UNKNOWN".equals(existingReceipt.status())) {
                checkouts.markRecoveryRequired(checkout.operationKey());
                return buildResult(getCheckout(checkout.operationKey()), existingReceipt,
                        "El resultado es desconocido. No realices otro cobro.");
            }
            if (existingReceipt.providerOrderId() != null
                    || "COLLECT_CREDENTIALS".equals(existingReceipt.nextAction())) {
                return buildResult(checkout, existingReceipt, existingReceipt.message());
            }
        }

        PaymentReceipt receipt;
        try {
            receipt = payments.initiate("CHECKOUT-" + checkout.operationKey(), checkout.totalAmount(),
                    "PEN", checkout.provider());
        } catch (RuntimeException exception) {
            log.error("No se pudo iniciar el pago del checkout {}", checkout.operationKey(), exception);
            return buildResult(checkout, null,
                    "No se confirmó la creación de la orden. Consulta esta operación antes de reintentar.");
        }

        if (!matchesReceipt(checkout, receipt)) {
            checkouts.markRecoveryRequired(checkout.operationKey());
            return buildResult(getCheckout(checkout.operationKey()), null,
                    "La respuesta de Payment no coincide con el checkout.");
        }

        if (checkout.paymentId() == null) {
            checkouts.linkPayment(checkout.operationKey(), receipt.paymentId());
        }
        CheckoutOperation linked = getCheckout(checkout.operationKey());

        if ("COMPLETED".equals(receipt.status())) return finish(userId, linked.operationKey(), receipt);
        if ("FAILED".equals(receipt.status())) {
            checkouts.cancelFailedCheckout(linked.operationKey(), receipt.paymentId());
            return buildResult(getCheckout(linked.operationKey()), receipt, "El pago fue rechazado.");
        }
        if ("UNKNOWN".equals(receipt.status())) {
            checkouts.markRecoveryRequired(linked.operationKey());
            return buildResult(getCheckout(linked.operationKey()), receipt,
                    "El resultado es desconocido. No realices otro cobro.");
        }
        return buildResult(linked, receipt, receipt.message());
    }

    @Override
    public CheckoutResult find(Long userId, String operationKey) {
        CheckoutOperation checkout = getCheckout(operationKey);
        validateOwner(userId, checkout);

        PaymentReceipt receipt = checkout.paymentId() == null ? null : payments.find(checkout.paymentId());
        return buildResult(checkout, receipt, "Estado actual del checkout.");
    }

    @Override
    public CheckoutResult verify(Long userId, String operationKey) {
        CheckoutOperation checkout = getCheckout(operationKey);
        validateOwner(userId, checkout);

        if (isTerminal(checkout.status())) return buildResult(checkout, null, "Operación finalizada.");

        if (checkout.paymentId() == null) {
            if ("RECOVERY_REQUIRED".equals(checkout.status())) {
                return buildResult(checkout, null, "Esta operación requiere revisión administrativa.");
            }
            return start(userId, reconstructRequest(checkout));
        }

        PaymentReceipt receipt;
        try {
            receipt = payments.verify(checkout.paymentId());
        } catch (RuntimeException exception) {
            log.error("Falló la consulta del pago del checkout {}", operationKey, exception);
            return buildResult(checkout, null,
                    "No se pudo consultar el pago. Se conserva la operación para revisión.");
        }

        if (!matchesReceipt(checkout, receipt)) {
            checkouts.markRecoveryRequired(operationKey);
            return buildResult(getCheckout(operationKey), null,
                    "La respuesta de verificación no coincide con el checkout.");
        }

        if ("COMPLETED".equals(receipt.status())) return finish(userId, operationKey, receipt);
        if ("FAILED".equals(receipt.status())) {
            checkouts.cancelFailedCheckout(operationKey, receipt.paymentId());
            return buildResult(getCheckout(operationKey), receipt, "Pago rechazado y reserva liberada.");
        }
        if ("UNKNOWN".equals(receipt.status())) {
            checkouts.markRecoveryRequired(operationKey);
            return buildResult(getCheckout(operationKey), receipt,
                    "No existe un resultado verificable. No inicies un segundo pago.");
        }
        return buildResult(checkout, receipt, receipt.message());
    }

    @Override
    public CheckoutResult submitCredentials(Long userId, String operationKey, CheckoutCredentials credentials) {
        if (credentials == null) throw new IllegalArgumentException("Faltan datos de autorización.");

        CheckoutOperation checkout = getCheckout(operationKey);
        validateOwner(userId, checkout);

        if (isTerminal(checkout.status())) return buildResult(checkout, null, "Operación finalizada.");
        if (!"PENDING_PAYMENT".equals(checkout.status())) {
            return buildResult(checkout, null, "Este checkout requiere revisión; no reenvíes el OTP.");
        }
        if (checkout.paymentId() == null) {
            throw new IllegalStateException("El checkout todavía no tiene una orden de pago vinculada.");
        }

        PaymentReceipt receipt = payments.submitCredentials(checkout.paymentId(), credentials);
        if (!matchesReceipt(checkout, receipt)) {
            checkouts.markRecoveryRequired(operationKey);
            return buildResult(getCheckout(operationKey), null,
                    "El resultado de autorización no coincide con el checkout.");
        }

        if ("COMPLETED".equals(receipt.status())) return finish(userId, operationKey, receipt);
        if ("FAILED".equals(receipt.status())) {
            checkouts.cancelFailedCheckout(operationKey, receipt.paymentId());
            return buildResult(getCheckout(operationKey), receipt, "Pago rechazado y reserva liberada.");
        }
        if ("UNKNOWN".equals(receipt.status())) {
            checkouts.markRecoveryRequired(operationKey);
            return buildResult(getCheckout(operationKey), receipt,
                    "El resultado es indeterminado. No envíes otro OTP.");
        }
        return buildResult(checkout, receipt, receipt.message());
    }

    @Override
    public CheckoutResult cancelForRecovery(Long userId, String operationKey) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Usuario inválido.");
        if (!simulatedYape) {
            throw new IllegalStateException("La cancelación de pruebas no está habilitada.");
        }

        CheckoutOperation checkout = getCheckout(operationKey);
        if ("CANCELLED".equals(checkout.status())) {
            return buildResult(checkout, null, "La operación ya estaba cancelada.");
        }
        if (!"RECOVERY_REQUIRED".equals(checkout.status()) || checkout.saleId() != null
                || !"YAPE".equalsIgnoreCase(checkout.provider()) || checkout.paymentId() == null) {
            throw new IllegalStateException("Solo se puede cancelar una prueba Yape bloqueada y sin venta.");
        }

        PaymentReceipt receipt = payments.find(checkout.paymentId());
        if (!matchesReceipt(checkout, receipt) || !"UNKNOWN".equals(receipt.status())
                || receipt.providerOrderId() != null || receipt.captureReference() != null
                || receipt.chargedAmount() != null) {
            throw new IllegalStateException("El pago exige reconciliación y no permite cancelación de prueba.");
        }

        // El repositorio vuelve a comprobar los datos bajo bloqueo SQL y libera la reserva atómicamente.
        checkouts.cancelSimulatedRecoveryCheckout(operationKey, checkout.paymentId());
        CheckoutOperation cancelled = getCheckout(operationKey);
        log.warn("Checkout Yape simulado cancelado administrativamente: {}", operationKey);
        return buildResult(cancelled, receipt, "Prueba cancelada. Se liberó el inventario reservado.");
    }

    private CheckoutResult finish(Long userId, String operationKey, PaymentReceipt receipt) {
        try {
            CheckoutOperation completed = finalization.finalizePaidCheckout(userId, operationKey, receipt);
            return buildResult(completed, receipt, "Pago confirmado y venta registrada.");
        } catch (RuntimeException exception) {
            log.error("Pago confirmado, pero falló la finalización de la venta {}", operationKey, exception);
            checkouts.markRecoveryRequired(operationKey);
            return buildResult(getCheckout(operationKey), receipt,
                    "El pago fue confirmado, pero la venta requiere recuperación. No vuelvas a cobrar.");
        }
    }

    private SaleRequest reconstructRequest(CheckoutOperation checkout) {
        List<CheckoutItem> lines = checkouts.findItems(checkout.operationKey());
        if (lines.isEmpty()) throw new IllegalStateException("El checkout no tiene productos.");
        return new SaleRequest(checkout.customerId(), checkout.paymentMethodId(), checkout.operationKey(),
                lines.stream().map(line -> new SaleRequest.Item(line.productId(), line.presentationId(),
                        line.quantity(), line.requestedBaseQuantity())).toList());
    }

    private boolean matchesReceipt(CheckoutOperation checkout, PaymentReceipt receipt) {
        return receipt != null && receipt.paymentId() != null
                && (checkout.paymentId() == null || checkout.paymentId().equals(receipt.paymentId()))
                && ("CHECKOUT-" + checkout.operationKey()).equals(receipt.operationReference())
                && checkout.provider().equalsIgnoreCase(receipt.provider())
                && receipt.originalAmount() != null
                && checkout.totalAmount().compareTo(receipt.originalAmount()) == 0
                && "PEN".equalsIgnoreCase(receipt.originalCurrency());
    }

    private CheckoutOperation getCheckout(String operationKey) {
        if (operationKey == null || operationKey.isBlank()) {
            throw new IllegalArgumentException("La referencia de checkout es obligatoria.");
        }
        return checkouts.findByKey(operationKey)
                .orElseThrow(() -> new NoSuchElementException("El checkout no existe."));
    }

    private void validateOwner(Long userId, CheckoutOperation checkout) {
        if (userId == null || !userId.equals(checkout.userId())) {
            throw new IllegalArgumentException("El checkout pertenece a otro usuario.");
        }
    }

    private boolean isTerminal(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private CheckoutResult buildResult(CheckoutOperation checkout, PaymentReceipt receipt, String message) {
        String action = "WAIT";
        if (isTerminal(checkout.status())) action = "NONE";
        else if ("RECOVERY_REQUIRED".equals(checkout.status())) action = "RECOVER";
        else if (receipt != null && "COLLECT_CREDENTIALS".equals(receipt.nextAction())) action = "COLLECT_CREDENTIALS";
        else if (receipt != null && receipt.qrBase64() != null) action = "DISPLAY_QR";
        else if (receipt != null && receipt.approvalUrl() != null) action = "OPEN_URL";

        return new CheckoutResult(checkout.operationKey(), checkout.status(), checkout.saleId(),
                checkout.paymentId(), receipt == null ? null : receipt.status(), checkout.totalAmount(),
                receipt == null ? null : receipt.qrBase64(),
                receipt == null ? null : receipt.approvalUrl(), action, message);
    }
}