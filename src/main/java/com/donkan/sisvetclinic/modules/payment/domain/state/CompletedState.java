package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class CompletedState implements PaymentState {

    @Override
    public String code() { return "COMPLETED"; }

    @Override
    public boolean isFinal() { return true; }
}