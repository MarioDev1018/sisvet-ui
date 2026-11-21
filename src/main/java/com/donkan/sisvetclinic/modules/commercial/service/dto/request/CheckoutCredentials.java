package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

public record CheckoutCredentials(
        String phoneNumber,
        String otp,
        String payerEmail
) {
}