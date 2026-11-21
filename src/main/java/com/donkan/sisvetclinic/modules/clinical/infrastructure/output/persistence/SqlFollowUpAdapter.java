package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.FollowUp;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.FollowUpRepositoryPort;

@Repository
public class SqlFollowUpAdapter implements FollowUpRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlFollowUpAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public FollowUp save(FollowUp followUp) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalFollowUp " +
            "(pet_id,consultation_id,reason,due_at,status," +
            "reviewed_at,appointment_id) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?)",
            Long.class,
            followUp.petId(), followUp.consultationId(),
            followUp.reason(), followUp.dueAt(),
            followUp.status(), followUp.reviewedAt(),
            followUp.appointmentId()
        );

        return new FollowUp(
            id, followUp.petId(), followUp.consultationId(),
            followUp.reason(), followUp.dueAt(),
            followUp.status(), followUp.reviewedAt(),
            followUp.appointmentId()
        );
    }

    public FollowUp findById(long id) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalFollowUp WHERE id=?",
            SqlClinicalMappers.FOLLOWUP, id
        ).stream().findFirst().orElse(null);
    }

    public List<FollowUp> findPending(LocalDateTime until) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalFollowUp " +
            "WHERE status='PENDING' AND due_at<=? ORDER BY due_at",
            SqlClinicalMappers.FOLLOWUP, until
        );
    }

    public List<FollowUp> findOutstanding() {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalFollowUp " +
            "WHERE status IN ('PENDING','REVIEWED') ORDER BY due_at",
            SqlClinicalMappers.FOLLOWUP
        );
    }

    public List<FollowUp> listByPet(long petId) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalFollowUp " +
            "WHERE pet_id=? ORDER BY due_at DESC",
            SqlClinicalMappers.FOLLOWUP, petId
        );
    }

    public void markReviewed(long id) {
        jdbc.update(
            "UPDATE dbo.ClinicalFollowUp SET " +
            "status='REVIEWED',reviewed_at=CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '-05:00') AS DATETIME2) " +
            "WHERE id=? AND status='PENDING'",
            id
        );
    }

    public void rescheduleByAppointment(
        long appointmentId,
        LocalDateTime date
    ) {
        jdbc.update(
            "UPDATE dbo.ClinicalFollowUp SET " +
            "due_at=?,reviewed_at=NULL,status='PENDING' " +
            "WHERE appointment_id=? AND status<>'COMPLETED'",
            date, appointmentId
        );
    }

    public void cancelByAppointment(long appointmentId) {
        jdbc.update(
            "UPDATE dbo.ClinicalFollowUp SET status='COMPLETED' " +
            "WHERE appointment_id=? AND status<>'COMPLETED'",
            appointmentId
        );
    }

    public void complete(long id) {
        int affected = jdbc.update(
            "UPDATE dbo.ClinicalFollowUp SET status='COMPLETED' " +
            "WHERE id=? AND status IN ('PENDING','REVIEWED')",
            id
        );

        if (affected != 1) {
            throw new IllegalStateException(
                "Seguimiento no disponible"
            );
        }
    }
}
