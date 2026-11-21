package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;

@RestController
@RequestMapping("/api/clinical/patients")
public class PatientController {

    private final PatientUseCase patients;
    private final ClinicalQueryUseCase queries;

    public PatientController(PatientUseCase patients) {
        this(patients, null);
    }

    @Autowired
    public PatientController(PatientUseCase patients, ClinicalQueryUseCase queries) {
        this.patients = patients;
        this.queries = queries;
    }

    @PostMapping
    public Pet registerPet(@RequestBody PetRequest request) {
        return patients.registerPet(request);
    }

    @GetMapping("/summaries")
    public List<PetSummary> summaries(
        @RequestParam(defaultValue = "") String search
    ) {
        return queries.searchPetSummaries(search);
    }

    @GetMapping("/{id}/summary")
    public PetSummary summary(@PathVariable long id) {
        return queries.findPetSummary(id);
    }

    @GetMapping("/{id}/history-view")
    public MedicalHistoryView historyView(@PathVariable long id) {
        return queries.getMedicalHistoryView(id);
    }

    @GetMapping("/{id}")
    public Pet findPet(@PathVariable long id) {
        return patients.findPet(id);
    }

    @GetMapping
    public List<Pet> search(
        @RequestParam(defaultValue = "") String search
    ) {
        return patients.searchPets(search);
    }

    @GetMapping("/{id}/history")
    public MedicalHistory history(@PathVariable long id) {
        return patients.getMedicalHistory(id);
    }

    @GetMapping("/{id}/consultations")
    public List<MedicalConsultation> consultations(
        @PathVariable long id
    ) {
        return patients.getConsultations(id);
    }
}
