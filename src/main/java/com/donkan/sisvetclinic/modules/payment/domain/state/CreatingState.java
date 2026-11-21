package com.donkan.sisvetclinic.modules.payment.domain.state;

public final class CreatingState implements PaymentState {

    @Override
    public String code() { return "CREATING"; }

    @Override
    public boolean canStartCreation() { return true; }

    @Override
    public PaymentState startCreation() {
        return this;
    }

    @Override
    public PaymentState orderCreated() {
        return new AwaitingApprovalState();
    }

    @Override
    public PaymentState creationUnknown() {
        return new UnknownState();
    }
    
    @Override
    public PaymentState finishImmediate(String result) {

        if (result == null) {
            return new UnknownState();
        }

        return switch (result) {
            case "COMPLETED" -> new CompletedState();
            case "FAILED" -> new FailedState();
            default -> new UnknownState();
        };
    }
}