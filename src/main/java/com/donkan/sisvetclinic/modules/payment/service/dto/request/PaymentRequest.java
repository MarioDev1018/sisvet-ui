
package com.donkan.sisvetclinic.modules.payment.service.dto.request;

import java.math.BigDecimal;

public record PaymentRequest(
        String operationReference,
        String provider,
        BigDecimal amount,
        String currency
) {
}
