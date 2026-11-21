package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalPaymentSession;

import java.util.Optional;

public interface ClinicalPaymentSessionPort {

    ClinicalPaymentSession saveOrGetExisting(ClinicalPaymentSession session);

    Optional<ClinicalPaymentSession> findByKey(String operationKey);

    void linkPayment(String operationKey, String paymentId);

    void complete(String operationKey, Long saleId);

    void cancel(String operationKey, String paymentId);

    void markRecoveryRequired(String operationKey);
}
