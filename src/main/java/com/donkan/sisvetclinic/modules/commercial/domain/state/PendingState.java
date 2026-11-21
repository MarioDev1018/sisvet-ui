package com.donkan.sisvetclinic.modules.commercial.domain.state;

public class PendingState implements InvoiceState {

    @Override
    public String getCode() {
        return "PENDING";
    }

    @Override
    public boolean canIssue() {
        return true;
    }

    @Override
    public boolean canVerify() {
        return false;
    }

    @Override
    public InvoiceState markPendingVerification() {
        return new PendingVerificationState();
    }

    @Override
    public InvoiceState accept() {
        throw new IllegalStateException("El comprobante todavía no ha sido enviado.");
    }

    @Override
    public InvoiceState reject() {
        throw new IllegalStateException("El comprobante todavía no ha sido enviado.");
    }
}