
package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

import java.math.BigDecimal;

public record InvoiceDetailResult(
        Long invoiceId,
        Long saleId,
        String documentType,
        String documentNumber,
        String customerName,
        BigDecimal totalAmount,
        String detail,
        String status,
        String responseCode,
        String message,
        String series,
        String number,
        String externalReference
) {
}
