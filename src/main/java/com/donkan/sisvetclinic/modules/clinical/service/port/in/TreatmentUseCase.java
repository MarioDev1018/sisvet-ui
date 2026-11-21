package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.VaccineProductOption;

public interface TreatmentUseCase {

    Treatment registerTreatment(TreatmentRequest request);

    ClinicalProcedure registerProcedure(ProcedureRequest request);

    Vaccination registerVaccination(VaccinationRequest request);

    Referral registerReferral(ReferralRequest request);

    java.util.List<MedicationDetail> getMedicationOptions(String search);

    default java.util.List<VaccineProductOption> getVaccineOptions(String search) {
        return java.util.List.of();
    }
}
