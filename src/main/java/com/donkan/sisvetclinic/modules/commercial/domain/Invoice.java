package com.donkan.sisvetclinic.modules.commercial.domain;

import com.donkan.sisvetclinic.modules.commercial.domain.state.InvoiceState;
import com.donkan.sisvetclinic.modules.commercial.domain.state.InvoiceStateFactory;
import com.donkan.sisvetclinic.modules.commercial.domain.state.PendingState;

public class Invoice {

    private Long id;
    private Long saleId;
    private String series;
    private String number;
    private String externalReference;
    private InvoiceState state;

    private Invoice(Long id, Long saleId, String series, String number,
                    String externalReference, InvoiceState state) {

        this.id = id;
        this.saleId = saleId;
        this.series = series;
        this.number = number;
        this.externalReference = externalReference;
        this.state = state;
    }

    public static Invoice pendingForSale(Long saleId) {

        if (saleId == null || saleId <= 0) {
            throw new IllegalArgumentException("La venta es inválida.");
        }

        return new Invoice(null, saleId, null, null, null, new PendingState());
    }

    public static Invoice restore(Long id, Long saleId, String series,
                                  String number, String externalReference, String stateCode) {

        return new Invoice(
                id,
                saleId,
                series,
                number,
                externalReference,
                InvoiceStateFactory.fromCode(stateCode)
        );
    }

    public boolean canIssue() {
        return state.canIssue();
    }

    public boolean canVerify() {
        return state.canVerify();
    }

    public void markPendingVerification() {
        state = state.markPendingVerification();
    }

    public void accept() {
        state = state.accept();
    }

    public void reject() {
        state = state.reject();
    }

    public String getStatusCode() {
        return state.getCode();
    }

    public InvoiceState getState() {
        return state;
    }

    public Long getId() {
        return id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }
}