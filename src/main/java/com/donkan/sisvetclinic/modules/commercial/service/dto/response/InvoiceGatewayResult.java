
package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

public record InvoiceGatewayResult(
        String status,
        String series,
        String number,
        String externalReference,
        String responseCode,
        String message
) {
}
