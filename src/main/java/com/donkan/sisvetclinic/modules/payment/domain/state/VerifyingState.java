package com.donkan.sisvetclinic.modules.payment.domain.state;

import java.util.Locale;

public final class VerifyingState implements PaymentState {

    @Override
    public String code() { return "VERIFYING"; }

    @Override
    public boolean canStartVerification() { return true; }

    @Override
    public PaymentState startVerification() {
        return this;
    }

    @Override
    public PaymentState resolveVerification(String status) {

        String result = status == null
                ? "UNKNOWN"
                : status.trim().toUpperCase(Locale.ROOT);

        return switch (result) {
            case "COMPLETED" -> new CompletedState();
            case "FAILED" -> new FailedState();
            case "AWAITING_APPROVAL" -> new AwaitingApprovalState();
            default -> new UnknownState();
        };
    }
}