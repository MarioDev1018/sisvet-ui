package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.PatientUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class PatientService implements PatientUseCase {

    private final PatientRepositoryPort patients;
    private final OwnerLookupPort owners;
    private final ClinicalRecordPort records;
    private final TreatmentRepositoryPort treatments;
    private final FollowUpRepositoryPort followUps;
    private final ActorPort actors;

    public PatientService(
        PatientRepositoryPort patients,
        OwnerLookupPort owners,
        ClinicalRecordPort records,
        TreatmentRepositoryPort treatments,
        FollowUpRepositoryPort followUps,
        ActorPort actors
    ) {
        this.patients = patients;
        this.owners = owners;
        this.records = records;
        this.treatments = treatments;
        this.followUps = followUps;
        this.actors = actors;
    }

    @Transactional
    public Pet registerPet(PetRequest r) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        if (r == null || r.ownerId() <= 0 || r.name() == null ||
            r.name().isBlank() || r.species() == null || r.species().isBlank()) {
            throw new IllegalArgumentException(
                "Propietario, nombre y especie son obligatorios"
            );
        }

        if (r.birthDate() == null &&
            (r.estimatedAgeMonths() == null || r.estimatedAgeMonths() < 0)) {
            throw new IllegalArgumentException(
                "Indique fecha de nacimiento o edad aproximada"
            );
        }

        if (!owners.existsOwner(r.ownerId())) {
            throw new IllegalArgumentException(
                "El propietario no existe o está inactivo"
            );
        }

        return patients.save(
            new Pet(
                0, r.ownerId(), r.name().trim(),
                ClinicalSpecies.canonicalCode(r.species()),
                r.breed(), r.sex(), r.birthDate(), r.estimatedAgeMonths()
            )
        );
    }

    public Pet findPet(long id) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        Pet pet = patients.findById(id);

        if (pet == null) {
            throw new IllegalArgumentException("Mascota no encontrada");
        }

        return pet;
    }

    public List<Pet> searchPets(String criteria) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        return patients.search(criteria == null ? "" : criteria.trim());
    }

    public MedicalHistory getMedicalHistory(long petId) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );

        if (patients.findById(petId) == null) {
            throw new IllegalArgumentException("Mascota no encontrada");
        }

        return new MedicalHistory(
            petId, petId,
            records.listByPet(petId),
            records.listDiagnosesByPet(petId),
            treatments.listTreatmentsByPet(petId),
            treatments.listProceduresByPet(petId),
            treatments.listVaccinations(petId),
            treatments.listReferralsByPet(petId),
            followUps.listByPet(petId)
        );
    }

    public List<MedicalConsultation> getConsultations(long petId) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );

        return records.listByPet(petId);
    }
}
