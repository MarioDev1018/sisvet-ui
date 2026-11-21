package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class UnknownState implements PaymentState {

    @Override
    public String code() { return "UNKNOWN"; }

    @Override
    public boolean canStartCreation() { return true; }

    @Override
    public boolean canStartVerification() { return true; }

    @Override
    public PaymentState startCreation() {
        return new CreatingState();
    }

    @Override
    public PaymentState startVerification() {
        return new VerifyingState();
    }
}