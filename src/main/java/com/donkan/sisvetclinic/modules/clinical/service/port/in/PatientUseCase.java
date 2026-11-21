package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;

public interface PatientUseCase {

    Pet registerPet(PetRequest request);

    Pet findPet(long id);

    List<Pet> searchPets(String criteria);

    MedicalHistory getMedicalHistory(long petId);

    List<MedicalConsultation> getConsultations(long petId);
}
