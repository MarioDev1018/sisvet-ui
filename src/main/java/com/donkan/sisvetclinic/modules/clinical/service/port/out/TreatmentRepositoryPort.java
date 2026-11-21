package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.time.LocalDate;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.*;

public interface TreatmentRepositoryPort {

    Treatment saveTreatment(Treatment treatment);

    ClinicalProcedure saveProcedure(ClinicalProcedure procedure);

    Vaccination saveVaccination(Vaccination vaccination);

    Referral saveReferral(Referral referral);

    List<Vaccination> listVaccinations(long petId);

    List<Treatment> listTreatmentsByPet(long petId);

    List<ClinicalProcedure> listProceduresByPet(long petId);

    List<Referral> listReferralsByPet(long petId);

    List<LocalDate> findPendingDoses(LocalDate date);
}
