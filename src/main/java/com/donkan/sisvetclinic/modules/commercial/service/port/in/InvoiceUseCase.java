package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.service.dto.response.InvoiceResult;

public interface InvoiceUseCase {

    InvoiceResult issueInvoice(Long saleId);

    String checkInvoiceStatus(Long invoiceId);
}