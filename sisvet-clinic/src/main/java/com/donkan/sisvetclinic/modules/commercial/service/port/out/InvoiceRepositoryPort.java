package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Invoice;

import java.util.Optional;

public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long invoiceId);

    Optional<Invoice> findBySaleId(Long saleId);

    boolean tryStartEmission(Long invoiceId);
}