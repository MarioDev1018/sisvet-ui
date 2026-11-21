package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import com.donkan.sisvetclinic.modules.clinical.domain.Pet;

public interface PatientUpdatePort {
    Pet update(long id, Pet pet);
}
