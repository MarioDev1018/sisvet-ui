package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Invoice;
import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.InvoiceResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.InvoiceUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.InvoiceGateway;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.InvoiceRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SaleRepositoryPort;

import java.util.Locale;
import java.util.NoSuchElementException;

public class InvoiceService implements InvoiceUseCase {

    private final InvoiceRepositoryPort invoices;
    private final SaleRepositoryPort sales;
    private final InvoiceGateway gateway;

    public InvoiceService(InvoiceRepositoryPort invoices, SaleRepositoryPort sales, InvoiceGateway gateway) {
        this.invoices = invoices;
        this.sales = sales;
        this.gateway = gateway;
    }

    @Override
    public InvoiceResult issueInvoice(Long saleId) {

        if (saleId == null || saleId <= 0) {
            throw new IllegalArgumentException("La venta es inválida.");
        }

        Sale sale = sales.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("La venta no existe."));

        if (!"Active".equalsIgnoreCase(sale.getRecordStatus())) {
            throw new IllegalStateException("No se puede emitir un comprobante para una venta inactiva.");
        }

        Invoice invoice = invoices.findBySaleId(saleId).orElse(null);

        if (invoice == null) {
            invoice = invoices.save(Invoice.pendingForSale(saleId));
        }

        if (!invoice.canIssue()) {
            return buildResult(invoice, "El comprobante ya tiene un proceso de emisión registrado.");
        }

        if (!invoices.tryStartEmission(invoice.getId())) {

            Invoice current = invoices.findById(invoice.getId())
                    .orElseThrow(() -> new NoSuchElementException("El comprobante no existe."));

            return buildResult(current, "El comprobante ya está siendo procesado.");
        }

        invoice.markPendingVerification();

        InvoiceResult externalResult;

        try {
            externalResult = gateway.issueInvoice(invoice);
        } catch (RuntimeException exception) {
            return buildResult(invoice, "No se pudo confirmar el resultado de la emisión.");
        }

        if (externalResult == null) {
            return buildResult(invoice, "El resultado de la emisión es desconocido.");
        }

        String status = normalizeStatus(externalResult.status());

        if ("ACCEPTED".equals(status)) {
            invoice.accept();
        } else if ("REJECTED".equals(status)) {
            invoice.reject();
        }

        if (externalResult.externalReference() != null) {
            invoice.setExternalReference(externalResult.externalReference());
        }

        invoice = invoices.save(invoice);

        return buildResult(invoice, externalResult.message());
    }

    @Override
    public String checkInvoiceStatus(Long invoiceId) {

        if (invoiceId == null || invoiceId <= 0) {
            throw new IllegalArgumentException("El comprobante es inválido.");
        }

        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("El comprobante no existe."));

        if (!invoice.canVerify()) {
            return invoice.getStatusCode();
        }

        String externalStatus;

        try {
            externalStatus = gateway.checkInvoiceStatus(invoice);
        } catch (RuntimeException exception) {
            return invoice.getStatusCode();
        }

        String status = normalizeStatus(externalStatus);

        if ("ACCEPTED".equals(status)) {
            invoice.accept();
        } else if ("REJECTED".equals(status)) {
            invoice.reject();
        } else {
            return invoice.getStatusCode();
        }

        invoice = invoices.save(invoice);

        return invoice.getStatusCode();
    }

    private String normalizeStatus(String status) {

        if (status == null) {
            return "";
        }

        return status.trim().toUpperCase(Locale.ROOT);
    }

    private InvoiceResult buildResult(Invoice invoice, String message) {

        return new InvoiceResult(
                invoice.getId(),
                invoice.getSaleId(),
                invoice.getStatusCode(),
                invoice.getExternalReference(),
                message
        );
    }
}