package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutItem;
import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutOperation;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;

import java.util.List;
import java.util.Optional;

public interface CheckoutRepositoryPort {

    record PaymentRoute(String processingType, String provider) {
    }

    PaymentRoute findPaymentRoute(Long paymentMethodId);

    CheckoutOperation reserve(Long userId, SaleRequest request, String provider);

    Optional<CheckoutOperation> findByKey(String operationKey);

    CheckoutOperation lockByKey(String operationKey);

    List<CheckoutItem> findItems(String operationKey);

    void linkPayment(String operationKey, String paymentId);

    void releaseReservation(String operationKey);

    void markCompleted(String operationKey, Long saleId);

    void markRecoveryRequired(String operationKey);

    void cancelFailedCheckout(String operationKey, String paymentId);

    void cancelSimulatedRecoveryCheckout(String operationKey, String paymentId);
}