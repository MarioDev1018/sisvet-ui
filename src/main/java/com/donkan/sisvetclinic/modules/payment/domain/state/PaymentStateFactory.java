package com.donkan.sisvetclinic.modules.payment.domain.state;

import java.util.Locale;

public final class PaymentStateFactory {

    private PaymentStateFactory() { }

    public static PaymentState fromCode(String code) {

        if (code == null) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }

        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> new PendingState();
            case "CREATING" -> new CreatingState();
            case "AWAITING_APPROVAL" -> new AwaitingApprovalState();
            case "VERIFYING" -> new VerifyingState();
            case "COMPLETED" -> new CompletedState();
            case "FAILED" -> new FailedState();
            case "UNKNOWN" -> new UnknownState();
            default -> throw new IllegalArgumentException(
                    "Estado de pago desconocido: " + code
            );
        };
    }
}