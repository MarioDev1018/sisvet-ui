package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

public record InvoiceResult(
        Long invoiceId,
        Long saleId,
        String status,
        String externalReference,
        String message
) {
}