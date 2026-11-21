package com.donkan.sisvetclinic.modules.commercial.domain.state;

public class PendingVerificationState implements InvoiceState {

    @Override
    public String getCode() {
        return "PENDING_VERIFICATION";
    }

    @Override
    public boolean canIssue() {
        return false;
    }

    @Override
    public boolean canVerify() {
        return true;
    }

    @Override
    public InvoiceState markPendingVerification() {
        return this;
    }

    @Override
    public InvoiceState accept() {
        return new AcceptedState();
    }

    @Override
    public InvoiceState reject() {
        return new RejectedState();
    }
}