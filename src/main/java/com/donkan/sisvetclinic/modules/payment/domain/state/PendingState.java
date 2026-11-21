package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class PendingState implements PaymentState {

    @Override
    public String code() { return "PENDING"; }

    @Override
    public boolean canStartCreation() { return true; }

    @Override
    public PaymentState startCreation() {
        return new CreatingState();
    }
}