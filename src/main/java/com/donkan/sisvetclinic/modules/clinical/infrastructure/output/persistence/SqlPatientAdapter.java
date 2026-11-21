package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.Pet;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.PatientRepositoryPort;

@Repository
public class SqlPatientAdapter implements PatientRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlPatientAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Pet save(Pet pet) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalPet " +
            "(owner_id,name,species,breed,sex,birth_date,estimated_age_months) " +
            "OUTPUT INSERTED.id VALUES (?,?,?,?,?,?,?)",
            Long.class,
            pet.ownerId(), pet.name(), pet.species(),
            pet.breed(), pet.sex(), pet.birthDate(),
            pet.estimatedAgeMonths()
        );

        return new Pet(
            id, pet.ownerId(), pet.name(), pet.species(),
            pet.breed(), pet.sex(), pet.birthDate(),
            pet.estimatedAgeMonths()
        );
    }

    public Pet findById(long id) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalPet WHERE id=?",
            SqlClinicalMappers.PET, id
        ).stream().findFirst().orElse(null);
    }

    public List<Pet> search(String criteria) {
        String term = "%" + criteria + "%";

        return jdbc.query(
            "SELECT p.* FROM dbo.ClinicalPet p " +
            "JOIN dbo.Customer c ON c.id=p.owner_id " +
            "WHERE p.name LIKE ? OR CAST(p.id AS VARCHAR(30)) LIKE ? " +
            "OR c.document_number LIKE ? ORDER BY p.name",
            SqlClinicalMappers.PET, term, term, term
        );
    }
}
