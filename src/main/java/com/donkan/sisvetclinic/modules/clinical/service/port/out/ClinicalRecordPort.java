package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.*;

public interface ClinicalRecordPort {

    MedicalConsultation saveConsultation(MedicalConsultation consultation);

    MedicalConsultation findConsultation(long id);

    List<MedicalConsultation> listByPet(long petId);

    List<Diagnosis> listDiagnosesByPet(long petId);

    Diagnosis saveDiagnosis(Diagnosis diagnosis);

    boolean hasCompleteDiagnosis(long consultationId);

    void completeConsultation(long id, long actorId);
}
