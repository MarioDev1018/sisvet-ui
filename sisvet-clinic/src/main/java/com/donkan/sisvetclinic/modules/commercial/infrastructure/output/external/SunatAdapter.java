package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.external;

import com.donkan.sisvetclinic.modules.commercial.domain.Invoice;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.InvoiceResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.InvoiceGateway;

import org.springframework.stereotype.Component;

@Component
public class SunatAdapter implements InvoiceGateway {

    @Override
    public InvoiceResult issueInvoice(Invoice invoice) {

        throw new UnsupportedOperationException(
                "La integración real con el proveedor SUNAT todavía no está configurada."
        );
    }

    @Override
    public String checkInvoiceStatus(Invoice invoice) {

        throw new UnsupportedOperationException(
                "La consulta externa de comprobantes todavía no está configurada."
        );
    }
}