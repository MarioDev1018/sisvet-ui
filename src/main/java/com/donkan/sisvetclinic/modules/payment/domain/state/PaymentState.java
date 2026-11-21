package com.donkan.sisvetclinic.modules.payment.domain.state;

public interface PaymentState {

    String code();

    default boolean canStartCreation() { return false; }

    default boolean canStartVerification() { return false; }

    default boolean isFinal() { return false; }

    default PaymentState startCreation() {
        throw invalid("crear orden");
    }

    default PaymentState orderCreated() {
        throw invalid("registrar orden creada");
    }

    default PaymentState creationUnknown() {
        throw invalid("registrar fallo de creación");
    }

    default PaymentState startVerification() {
        throw invalid("verificar");
    }

    default PaymentState resolveVerification(String status) {
        throw invalid("aplicar verificación");
    }

    private IllegalStateException invalid(String operation) {
        return new IllegalStateException(
                "No se puede " + operation + " desde " + code()
        );
    }
    
    default PaymentState finishImmediate(String result) {
    throw new IllegalStateException(
            "El estado " + code() + " no admite pagos inmediatos."
    );
}
}