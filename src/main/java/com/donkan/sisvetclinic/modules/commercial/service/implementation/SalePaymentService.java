
package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SalePaymentUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort.PaymentReceipt;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SaleRepositoryPort;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

public class SalePaymentService implements SalePaymentUseCase {

    private final SaleRepositoryPort sales;
    private final PaymentProcessingPort payments;

    public SalePaymentService(SaleRepositoryPort sales, PaymentProcessingPort payments) {
        this.sales = sales;
        this.payments = payments;
    }

    @Override
    public PaymentReceipt initiateForSale(Long saleId, String provider) {

        Sale sale = findSale(saleId);

        if (!"Active".equalsIgnoreCase(sale.getRecordStatus())) {
            throw new IllegalStateException("La venta no está activa.");
        }

        if (sale.getTotalAmount() == null ||
                sale.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La venta no tiene un importe válido.");
        }

        return payments.initiate(
                "SALE-" + sale.getId(),
                sale.getTotalAmount(),
                "PEN",
                provider
        );
    }

    @Override
    public PaymentReceipt submitCredentialsForSale(Long saleId, String paymentId, CheckoutCredentials credentials) {

        findSale(saleId);
        PaymentReceipt receipt = payments.find(paymentId);
        validateOwnership(saleId, receipt);

        return payments.submitCredentials(paymentId, credentials);
    }

    @Override
    public PaymentReceipt verifyForSale(Long saleId, String paymentId) {

        findSale(saleId);

        PaymentReceipt receipt = payments.find(paymentId);
        validateOwnership(saleId, receipt);

        return payments.verify(paymentId);
    }

    @Override
    public PaymentReceipt findForSale(Long saleId, String paymentId) {

        findSale(saleId);

        PaymentReceipt receipt = payments.find(paymentId);
        validateOwnership(saleId, receipt);

        return receipt;
    }

    private Sale findSale(Long saleId) {

        if (saleId == null || saleId <= 0) {
            throw new IllegalArgumentException("La venta es inválida.");
        }

        return sales.findById(saleId)
                .orElseThrow(() -> new NoSuchElementException("La venta no existe."));
    }

    private void validateOwnership(Long saleId, PaymentReceipt receipt) {

        if (!("SALE-" + saleId).equals(receipt.operationReference())) {
            throw new IllegalArgumentException("El pago no corresponde a la venta indicada.");
        }
    }
}
