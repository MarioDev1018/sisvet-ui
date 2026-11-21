package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.commercial;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.ProfessionalLookupPort;

@Repository
public class CommercialProfessionalAdapter implements ProfessionalLookupPort {

    private final JdbcTemplate jdbc;

    public CommercialProfessionalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean canAttend(long id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.[User] u " +
            "JOIN dbo.Role r ON r.id=u.role_id " +
            "WHERE u.id=? AND u.record_status='Active' " +
            "AND UPPER(r.name) IN " +
            "('VETERINARIO','VETERINARIAN','ESPECIALISTA','SPECIALIST')",
            Integer.class, id
        );

        return count != null && count > 0;
    }

    public boolean hasSpecialty(long id, String specialty) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalProfessionalSpecialty " +
            "WHERE professional_id=? AND specialty=?",
            Integer.class, id, specialty
        );

        return count != null && count > 0;
    }

    @Override
    public void assignSpecialty(long professionalId, String specialty) {
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalProfessionalSpecialty " +
            "WHERE professional_id=? AND specialty=?",
            Integer.class, professionalId, specialty
        );

        if (existing == null || existing == 0) {
            jdbc.update(
                "INSERT INTO dbo.ClinicalProfessionalSpecialty (professional_id, specialty) VALUES (?, ?)",
                professionalId, specialty
            );
        }
    }

    @Override
    public void removeSpecialty(long professionalId, String specialty) {
        jdbc.update(
            "DELETE FROM dbo.ClinicalProfessionalSpecialty WHERE professional_id=? AND specialty=?",
            professionalId, specialty
        );
    }
}
