package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;
import java.util.List;

public record ClinicalPaymentSession(
        String operationKey,
        Long userId,
        Long consultationId,
        Long attentionId,
        Long customerId,
        Long paymentMethodId,
        String provider,
        List<Long> chargeIds,
        BigDecimal totalAmount,
        String fingerprint,
        String status,
        String paymentId,
        Long saleId
) {}
