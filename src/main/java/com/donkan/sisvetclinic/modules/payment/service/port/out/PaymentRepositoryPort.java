package com.donkan.sisvetclinic.modules.payment.service.port.out;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import java.util.Optional;

public interface PaymentRepositoryPort {

    boolean insertIfAbsent(PaymentOrder order);

    void update(PaymentOrder order, String expectedStatus);

    boolean tryStartCreation(String paymentId);

    boolean tryStartVerification(String paymentId);

    Optional<PaymentOrder> findById(String paymentId);

    Optional<PaymentOrder> findByOperationReference(String reference);

    Optional<PaymentOrder> findByProviderOrderId(String providerOrderId);

    String simulationDecision(String providerOrderId);

    void decideSimulation(String providerOrderId, String decision);
}