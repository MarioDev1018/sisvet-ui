package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.AttentionRepositoryPort;

@Repository
public class SqlAttentionAdapter implements AttentionRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlAttentionAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ClinicalAttention save(ClinicalAttention attention) {
        if (attention.getId() == 0) {
            Long id = jdbc.queryForObject(
                "INSERT INTO dbo.ClinicalAttention " +
                "(pet_id,type,priority,urgency_description," +
                "priority_assigned_by,state,arrived_at,requires_grooming," +
                "clinical_closed,consultation_id,vaccination_completed) " +
                "OUTPUT INSERTED.id VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                Long.class,
                attention.getPetId(), attention.getType(), attention.getPriority(),
                attention.getUrgencyDescription(), attention.getPriorityAssignedBy(),
                attention.getStatus(), attention.getArrivedAt(),
                attention.isRequiresGrooming(), attention.isClinicalClosed(),
                attention.getConsultationId(), attention.isVaccinationCompleted()
            );
            attention.setId(id);
        } else {
            int affected = jdbc.update(
                "UPDATE dbo.ClinicalAttention SET " +
                "type=?,priority=?,urgency_description=?," +
                "priority_assigned_by=?,state=?,arrived_at=?," +
                "requires_grooming=?,clinical_closed=?,consultation_id=?," +
                "vaccination_completed=?,[version]=[version]+1 " +
                "WHERE id=? AND [version]=?",
                attention.getType(), attention.getPriority(),
                attention.getUrgencyDescription(), attention.getPriorityAssignedBy(),
                attention.getStatus(), attention.getArrivedAt(),
                attention.isRequiresGrooming(), attention.isClinicalClosed(),
                attention.getConsultationId(), attention.isVaccinationCompleted(),
                attention.getId(), attention.getVersion()
            );

            if (affected != 1) {
                throw new IllegalStateException(
                    "La atención fue modificada por otro usuario. Recargue y reintente"
                );
            }

            attention.incrementVersion();
        }

        return attention;
    }

    public ClinicalAttention findById(long id) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalAttention WHERE id=?",
            SqlClinicalMappers.ATTENTION, id
        ).stream().findFirst().orElse(null);
    }

    public ClinicalAttention findOpenByPet(long petId) {
        return jdbc.query(
            "SELECT TOP 1 * FROM dbo.ClinicalAttention " +
            "WHERE pet_id=? AND state<>'ATENDIDO' ORDER BY id DESC",
            SqlClinicalMappers.ATTENTION, petId
        ).stream().findFirst().orElse(null);
    }

    public List<ClinicalAttention> findWaiting() {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalAttention " +
            "WHERE state='EN_ESPERA' AND arrived_at IS NOT NULL " +
            "ORDER BY CASE WHEN priority IS NULL THEN 1 ELSE 0 END, " +
            "priority, arrived_at",
            SqlClinicalMappers.ATTENTION
        );
    }

    public void auditPriority(long attentionId, long veterinarianId, int priority) {
        jdbc.update(
            "INSERT INTO dbo.ClinicalPriorityAudit " +
            "(attention_id,veterinarian_id,priority,changed_at) " +
            "VALUES(?,?,?,CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2))",
            attentionId, veterinarianId, priority
        );
    }
}
