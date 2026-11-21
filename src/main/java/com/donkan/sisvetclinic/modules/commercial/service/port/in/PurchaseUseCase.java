package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.PurchaseRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.PurchaseResult;

import java.util.List;

public interface PurchaseUseCase {

    PurchaseResult registerPurchase(Long userId, PurchaseRequest request);

    List<PaymentMethod> listPaymentMethods();
}