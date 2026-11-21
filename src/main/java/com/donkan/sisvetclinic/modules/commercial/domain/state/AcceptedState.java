package com.donkan.sisvetclinic.modules.commercial.domain.state;

public class AcceptedState implements InvoiceState {

    @Override
    public String getCode() {
        return "ACCEPTED";
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
        throw new IllegalStateException("El comprobante ya fue aceptado.");
    }

    @Override
    public InvoiceState accept() {
        return this;
    }

    @Override
    public InvoiceState reject() {
        throw new IllegalStateException("Un comprobante aceptado no puede rechazarse.");
    }
}