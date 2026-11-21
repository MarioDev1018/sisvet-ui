package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.Pet;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.PatientUpdateUseCase;

@RestController
@RequestMapping("/api/clinical/patients")
public class PatientUpdateController {

    private final PatientUpdateUseCase patients;

    public PatientUpdateController(
        PatientUpdateUseCase patients
    ) {
        this.patients = patients;
    }

    @PutMapping("/{id}")
    public Pet updatePet(
        @PathVariable long id,
        @RequestBody PetRequest request
    ) {
        return patients.updatePet(id, request);
    }
}
