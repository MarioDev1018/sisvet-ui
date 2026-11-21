package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class AwaitingApprovalState implements PaymentState {

    @Override
    public String code() { return "AWAITING_APPROVAL"; }

    @Override
    public boolean canStartVerification() { return true; }

    @Override
    public PaymentState startVerification() {
        return new VerifyingState();
    }
}