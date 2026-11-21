package com.donkan.sisvetclinic.modules.commercial.domain.state;

public class RejectedState implements InvoiceState {

    @Override
    public String getCode() {
        return "REJECTED";
    }

    @Override
    public boolean canIssue() {
        return false;
    }

    @Override
    public boolean canVerify() {
        return false;
    }

    @Override
    public InvoiceState markPendingVerification() {
        throw new IllegalStateException("El comprobante fue rechazado.");
    }

    @Override
    public InvoiceState accept() {
        throw new IllegalStateException("Un comprobante rechazado no puede aceptarse directamente.");
    }

    @Override
    public InvoiceState reject() {
        return this;
    }
}