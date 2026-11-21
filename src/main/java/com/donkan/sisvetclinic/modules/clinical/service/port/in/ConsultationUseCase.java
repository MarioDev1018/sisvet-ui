package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightCheckResult;

public interface ConsultationUseCase {

    MedicalConsultation registerConsultation(ConsultationRequest request);

    Diagnosis registerDiagnosis(DiagnosisRequest request);

    void completeConsultation(long id);

    String checkWeight(long petId, java.math.BigDecimal weight);

    WeightCheckResult evaluateWeight(long petId, java.math.BigDecimal weight);
}
