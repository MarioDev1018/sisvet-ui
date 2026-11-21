package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutItem;
import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutOperation;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SaleUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CheckoutRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort.PaymentReceipt;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

public class CheckoutFinalizationService {

    private final CheckoutRepositoryPort checkouts;
    private final SaleUseCase sales;

    public CheckoutFinalizationService(CheckoutRepositoryPort checkouts, SaleUseCase sales) {
        this.checkouts = checkouts;
        this.sales = sales;
    }

    @Transactional
    public CheckoutOperation finalizePaidCheckout(Long userId, String operationKey,
                                                   PaymentReceipt payment) {

        CheckoutOperation checkout = checkouts.lockByKey(operationKey);

        if (!checkout.userId().equals(userId)) {
            throw new IllegalArgumentException("La operación no pertenece al usuario.");
        }

        if ("COMPLETED".equals(checkout.status())) {
            return checkout;
        }

        if (!"PENDING_PAYMENT".equals(checkout.status())
                && !"RECOVERY_REQUIRED".equals(checkout.status())) {
            throw new IllegalStateException("El checkout no admite finalización.");
        }

        if (payment == null || !"COMPLETED".equals(payment.status())
                || !payment.paymentId().equals(checkout.paymentId())
                || !("CHECKOUT-" + operationKey).equals(payment.operationReference())
                || !checkout.provider().equalsIgnoreCase(payment.provider())
                || payment.originalAmount() == null
                || payment.originalAmount().compareTo(checkout.totalAmount()) != 0
                || !"PEN".equalsIgnoreCase(payment.originalCurrency())
                || payment.captureReference() == null
                || payment.captureReference().isBlank()) {

            throw new IllegalStateException(
                    "La captura no coincide con el checkout registrado."
            );
        }

        List<CheckoutItem> lines = checkouts.findItems(operationKey);

        if (lines.isEmpty()) {
            throw new IllegalStateException("El checkout no tiene productos.");
        }

        SaleRequest request = new SaleRequest(
                checkout.customerId(),
                checkout.paymentMethodId(),
                operationKey,
                lines.stream()
                        .map(line -> new SaleRequest.Item(
                                line.productId(),
                                line.presentationId(),
                                line.quantity(),
                                line.requestedBaseQuantity()
                        ))
                        .toList()
        );

        // Se devuelve la reserva en esta misma transacción.
        checkouts.releaseReservation(operationKey);

        // SaleService vuelve a descontar el stock como venta definitiva.
        SaleResult result = sales.registerSale(userId, request);

        if (result == null || result.saleId() == null
                || result.totalAmount() == null
                || result.totalAmount().compareTo(checkout.totalAmount()) != 0) {

            throw new IllegalStateException(
                    "La venta no coincide con el importe capturado."
            );
        }

        checkouts.markCompleted(operationKey, result.saleId());

        return checkouts.findByKey(operationKey).orElseThrow();
    }
}