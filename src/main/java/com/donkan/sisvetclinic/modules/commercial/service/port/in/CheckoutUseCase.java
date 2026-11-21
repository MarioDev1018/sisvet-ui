package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.CheckoutResult;

public interface CheckoutUseCase {

    CheckoutResult start(Long userId, SaleRequest request);

    CheckoutResult find(Long userId, String operationKey);

    CheckoutResult verify(Long userId, String operationKey);

    CheckoutResult submitCredentials(Long userId, String operationKey, CheckoutCredentials credentials);

    CheckoutResult cancelForRecovery(Long userId, String operationKey);
}