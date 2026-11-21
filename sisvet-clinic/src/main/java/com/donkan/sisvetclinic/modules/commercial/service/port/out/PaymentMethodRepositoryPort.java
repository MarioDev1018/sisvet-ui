package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepositoryPort {

    Optional<PaymentMethod> findById(Long paymentMethodId);

    List<PaymentMethod> findAllActive();
}