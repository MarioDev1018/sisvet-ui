package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.ClinicalRecordPort;

@Repository
public class SqlClinicalRecordAdapter implements ClinicalRecordPort {

    private final JdbcTemplate jdbc;

    public SqlClinicalRecordAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MedicalConsultation saveConsultation(MedicalConsultation c) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.MedicalConsultation " +
            "(attention_id,pet_id,veterinarian_id,reason,weight," +
            "symptoms,observations,status,created_at) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,?)",
            Long.class,
            c.attentionId(), c.petId(), c.veterinarianId(),
            c.reason(), c.weight(), c.symptoms(),
            c.observations(), c.status(), c.createdAt()
        );

        return new MedicalConsultation(
            id, c.attentionId(), c.petId(), c.veterinarianId(),
            c.reason(), c.weight(), c.symptoms(),
            c.observations(), c.status(), c.createdAt()
        );
    }

    public MedicalConsultation findConsultation(long id) {
        return jdbc.query(
            "SELECT * FROM dbo.MedicalConsultation WHERE id=?",
            SqlClinicalMappers.CONSULTATION, id
        ).stream().findFirst().orElse(null);
    }

    public List<MedicalConsultation> listByPet(long petId) {
        return jdbc.query(
            "SELECT * FROM dbo.MedicalConsultation " +
            "WHERE pet_id=? ORDER BY created_at DESC",
            SqlClinicalMappers.CONSULTATION, petId
        );
    }

    public List<Diagnosis> listDiagnosesByPet(long petId) {
        return jdbc.query(
            "SELECT d.* FROM dbo.ClinicalDiagnosis d " +
            "JOIN dbo.MedicalConsultation c " +
            "ON c.id=d.consultation_id " +
            "WHERE c.pet_id=? ORDER BY d.created_at DESC",
            SqlClinicalMappers.DIAGNOSIS, petId
        );
    }

    public Diagnosis saveDiagnosis(Diagnosis diagnosis) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalDiagnosis " +
            "(consultation_id,description,indications,author_id,created_at) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?)",
            Long.class,
            diagnosis.consultationId(),
            diagnosis.description(),
            diagnosis.indications(),
            diagnosis.authorId(),
            diagnosis.createdAt()
        );

        return new Diagnosis(
            id, diagnosis.consultationId(),
            diagnosis.description(), diagnosis.indications(),
            diagnosis.authorId(), diagnosis.createdAt()
        );
    }

    public boolean hasCompleteDiagnosis(long consultationId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalDiagnosis " +
            "WHERE consultation_id=? " +
            "AND NULLIF(LTRIM(RTRIM(description)),'') IS NOT NULL " +
            "AND NULLIF(LTRIM(RTRIM(indications)),'') IS NOT NULL",
            Integer.class, consultationId
        );

        return count != null && count > 0;
    }

    public void completeConsultation(long id, long actorId) {
        int affected = jdbc.update(
            "UPDATE dbo.MedicalConsultation SET " +
            "status='COMPLETED',last_modified_by=?," +
            "last_modified_at=CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2) " +
            "WHERE id=? AND status='OPEN'",
            actorId, id
        );

        if (affected != 1) {
            throw new IllegalStateException(
                "Consulta finalizada o modificada simultáneamente"
            );
        }
    }
}
