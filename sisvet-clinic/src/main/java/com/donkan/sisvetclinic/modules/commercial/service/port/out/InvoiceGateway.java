package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Invoice;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.InvoiceResult;

public interface InvoiceGateway {

    InvoiceResult issueInvoice(Invoice invoice);

    String checkInvoiceStatus(Invoice invoice);
}