package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.Pet;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.PatientUpdatePort;

@Repository
public class SqlPatientUpdateAdapter implements PatientUpdatePort {

    private final JdbcTemplate jdbc;

    public SqlPatientUpdateAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pet update(long id, Pet pet) {
        int affected = jdbc.update(
            "UPDATE dbo.ClinicalPet SET " +
            "owner_id=?,name=?,species=?,breed=?,sex=?," +
            "birth_date=?,estimated_age_months=? WHERE id=?",
            pet.ownerId(),
            pet.name(),
            pet.species(),
            pet.breed(),
            pet.sex(),
            pet.birthDate(),
            pet.estimatedAgeMonths(),
            id
        );

        if (affected != 1) {
            throw new IllegalStateException(
                "No se pudo modificar la mascota"
            );
        }

        return jdbc.query(
            "SELECT * FROM dbo.ClinicalPet WHERE id=?",
            SqlClinicalMappers.PET, id
        ).stream().findFirst().orElseThrow(
            () -> new IllegalStateException(
                "Registro actualizado no encontrado"
            )
        );
    }
}
