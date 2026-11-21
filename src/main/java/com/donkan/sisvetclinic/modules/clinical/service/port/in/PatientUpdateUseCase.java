package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import com.donkan.sisvetclinic.modules.clinical.domain.Pet;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;

public interface PatientUpdateUseCase {
    Pet updatePet(long id, PetRequest request);
}
