package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.Pet;

public interface PatientRepositoryPort {

    Pet save(Pet pet);

    Pet findById(long id);

    List<Pet> search(String criteria);
}
