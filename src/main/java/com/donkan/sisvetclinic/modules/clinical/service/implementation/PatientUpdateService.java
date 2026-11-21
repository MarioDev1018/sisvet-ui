package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalSpecies;
import com.donkan.sisvetclinic.modules.clinical.domain.Pet;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.PetRequest;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.PatientUpdateUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class PatientUpdateService implements PatientUpdateUseCase {

    private final PatientRepositoryPort patients;
    private final PatientUpdatePort updates;
    private final OwnerLookupPort owners;
    private final ActorPort actors;

    public PatientUpdateService(
        PatientRepositoryPort patients,
        PatientUpdatePort updates,
        OwnerLookupPort owners,
        ActorPort actors
    ) {
        this.patients = patients;
        this.updates = updates;
        this.owners = owners;
        this.actors = actors;
    }

    @Transactional
    public Pet updatePet(long id, PetRequest request) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        Pet current = patients.findById(id);

        if (current == null) {
            throw new IllegalArgumentException("Mascota inexistente");
        }

        if (request == null) {
            throw new IllegalArgumentException("Indique los datos de la mascota");
        }

        if ((request.name() != null && request.name().isBlank()) ||
            (request.species() != null && request.species().isBlank()) ||
            (request.estimatedAgeMonths() != null && request.estimatedAgeMonths() < 0)) {
            throw new IllegalArgumentException("Datos de mascota inválidos");
        }

        long ownerId = request.ownerId() > 0 ? request.ownerId() : current.ownerId();
        String name = request.name() != null ? request.name().trim() : current.name();
        String species = request.species() != null
            ? ClinicalSpecies.canonicalCode(request.species())
            : current.species();
        String breed = request.breed() != null ? request.breed() : current.breed();
        String sex = request.sex() != null ? request.sex() : current.sex();
        var birthDate = request.birthDate() != null ? request.birthDate() : current.birthDate();
        Integer age = request.estimatedAgeMonths() != null
            ? request.estimatedAgeMonths() : current.estimatedAgeMonths();

        if (!owners.existsOwner(ownerId)) {
            throw new IllegalArgumentException("Propietario inexistente");
        }

        if (birthDate == null && age == null) {
            throw new IllegalArgumentException(
                "Indique fecha de nacimiento o edad aproximada"
            );
        }

        Pet changed = new Pet(
            id, ownerId, name, species, breed, sex, birthDate, age
        );

        return updates.update(id, changed);
    }
}
