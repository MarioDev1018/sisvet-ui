package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalCharge;

import java.util.List;

public interface ClinicalChargePort {

    List<ClinicalCharge> findPendingByConsultation(Long consultationId);

    void markAsCharged(List<Long> chargeIds, Long saleId);
}