package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

import java.util.List;

public record ClinicalPaymentRequest(
        Long consultationId,
        Long customerId,
        Long paymentMethodId,
        String operationKey,
        List<Long> chargeIds
) {
}