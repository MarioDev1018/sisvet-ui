package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class FailedState implements PaymentState {

    @Override
    public String code() { return "FAILED"; }

    @Override
    public boolean isFinal() { return true; }
}