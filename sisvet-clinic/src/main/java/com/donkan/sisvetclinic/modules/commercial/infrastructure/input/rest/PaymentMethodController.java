package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.PurchaseUseCase;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PurchaseUseCase purchases;

    public PaymentMethodController(PurchaseUseCase purchases) {
        this.purchases = purchases;
    }

    @GetMapping
    public List<PaymentMethod> listPaymentMethods() {
        return purchases.listPaymentMethods();
    }
}