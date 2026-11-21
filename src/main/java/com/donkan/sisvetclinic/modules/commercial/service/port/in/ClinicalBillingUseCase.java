package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalCharge;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.ClinicalPaymentRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;

import java.util.List;

public interface ClinicalBillingUseCase {

    List<ClinicalCharge> getPendingCharges(Long consultationId);

    SaleResult registerClinicalPayment(Long userId, ClinicalPaymentRequest request);
}