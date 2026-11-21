package com.donkan.sisvetclinic.modules.payment.service.dto.request;

public record PaymentCredentials(
        String phoneNumber,
        String otp,
        String payerEmail
) {
    public PaymentCredentials {
        if (phoneNumber == null || !phoneNumber.matches("[0-9]{9}")) {
            throw new IllegalArgumentException("El celular debe tener nueve dígitos.");
        }

        if (otp == null || !otp.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("El OTP debe tener seis dígitos.");
        }

        if (payerEmail == null || payerEmail.isBlank()
                || !payerEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo del pagador no es válido.");
        }
    }
}