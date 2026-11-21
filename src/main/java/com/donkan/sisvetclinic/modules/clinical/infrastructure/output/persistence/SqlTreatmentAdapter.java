package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.TreatmentRepositoryPort;

@Repository
public class SqlTreatmentAdapter implements TreatmentRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlTreatmentAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Treatment saveTreatment(Treatment treatment) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalTreatment " +
            "(consultation_id,description,instructions,product_id," +
            "prescribed_at,author_id) OUTPUT INSERTED.id " +
            "VALUES(?,?,?,?,?,?)",
            Long.class,
            treatment.consultationId(), treatment.description(),
            treatment.instructions(), treatment.productId(),
            treatment.prescribedAt(), treatment.authorId()
        );

        return new Treatment(
            id, treatment.consultationId(), treatment.description(),
            treatment.instructions(), treatment.productId(),
            treatment.prescribedAt(), treatment.authorId()
        );
    }

    public ClinicalProcedure saveProcedure(ClinicalProcedure procedure) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalProcedure " +
            "(consultation_id,name,observations,performed_at,author_id) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?)",
            Long.class,
            procedure.consultationId(), procedure.name(),
            procedure.observations(), procedure.performedAt(),
            procedure.authorId()
        );

        return new ClinicalProcedure(
            id, procedure.consultationId(), procedure.name(),
            procedure.observations(), procedure.performedAt(),
            procedure.authorId()
        );
    }

    public Vaccination saveVaccination(Vaccination vaccination) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalVaccination " +
            "(pet_id,consultation_id,attention_id,vaccine,dose,applied_at," +
            "next_dose_date,veterinarian_id,product_id,quantity_used) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,?,?)",
            Long.class,
            vaccination.petId(), vaccination.consultationId(),
            vaccination.attentionId(), vaccination.vaccine(), vaccination.dose(),
            vaccination.appliedAt(), vaccination.nextDoseDate(),
            vaccination.veterinarianId(), vaccination.productId(),
            vaccination.quantityUsed()
        );

        return new Vaccination(
            id, vaccination.petId(), vaccination.consultationId(),
            vaccination.attentionId(), vaccination.vaccine(), vaccination.dose(),
            vaccination.appliedAt(), vaccination.nextDoseDate(),
            vaccination.veterinarianId(), vaccination.productId(),
            vaccination.quantityUsed()
        );
    }

    public Referral saveReferral(Referral referral) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalReferral " +
            "(consultation_id,reason,destination,created_at,author_id) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?)",
            Long.class,
            referral.consultationId(), referral.reason(),
            referral.destination(), referral.createdAt(),
            referral.authorId()
        );

        return new Referral(
            id, referral.consultationId(), referral.reason(),
            referral.destination(), referral.createdAt(),
            referral.authorId()
        );
    }

    public List<Treatment> listTreatmentsByPet(long petId) {
        return jdbc.query(
            "SELECT t.* FROM dbo.ClinicalTreatment t " +
            "JOIN dbo.MedicalConsultation c ON c.id=t.consultation_id " +
            "WHERE c.pet_id=? ORDER BY t.prescribed_at DESC",
            SqlClinicalMappers.TREATMENT, petId
        );
    }

    public List<ClinicalProcedure> listProceduresByPet(long petId) {
        return jdbc.query(
            "SELECT p.* FROM dbo.ClinicalProcedure p " +
            "JOIN dbo.MedicalConsultation c ON c.id=p.consultation_id " +
            "WHERE c.pet_id=? ORDER BY p.performed_at DESC",
            SqlClinicalMappers.PROCEDURE, petId
        );
    }

    public List<Referral> listReferralsByPet(long petId) {
        return jdbc.query(
            "SELECT r.* FROM dbo.ClinicalReferral r " +
            "JOIN dbo.MedicalConsultation c ON c.id=r.consultation_id " +
            "WHERE c.pet_id=? ORDER BY r.created_at DESC",
            SqlClinicalMappers.REFERRAL, petId
        );
    }

    public List<Vaccination> listVaccinations(long petId) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalVaccination " +
            "WHERE pet_id=? ORDER BY applied_at DESC",
            SqlClinicalMappers.VACCINATION, petId
        );
    }

    public List<LocalDate> findPendingDoses(LocalDate date) {
        return jdbc.query(
            "SELECT next_dose_date FROM dbo.ClinicalVaccination " +
            "WHERE next_dose_date<=?",
            (r, n) -> r.getDate(1).toLocalDate(),
            date
        );
    }
}
