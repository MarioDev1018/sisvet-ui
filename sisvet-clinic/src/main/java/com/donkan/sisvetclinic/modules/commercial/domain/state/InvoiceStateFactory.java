package com.donkan.sisvetclinic.modules.commercial.domain.state;

public final class InvoiceStateFactory {

    private static final InvoiceState PENDING = new PendingState();
    private static final InvoiceState PENDING_VERIFICATION = new PendingVerificationState();
    private static final InvoiceState ACCEPTED = new AcceptedState();
    private static final InvoiceState REJECTED = new RejectedState();

    private InvoiceStateFactory() {
    }

    public static InvoiceState fromCode(String code) {

        if (code == null) {
            throw new IllegalArgumentException("El estado del comprobante es obligatorio.");
        }

        return switch (code.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PENDING" -> PENDING;
            case "PENDING_VERIFICATION" -> PENDING_VERIFICATION;
            case "ACCEPTED" -> ACCEPTED;
            case "REJECTED" -> REJECTED;
            default -> throw new IllegalArgumentException("Estado desconocido: " + code);
        };
    }
}