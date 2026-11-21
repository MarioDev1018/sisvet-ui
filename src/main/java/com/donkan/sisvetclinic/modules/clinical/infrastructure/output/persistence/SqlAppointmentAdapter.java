package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.Appointment;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.AppointmentRepositoryPort;

@Repository
public class SqlAppointmentAdapter implements AppointmentRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlAppointmentAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Appointment save(Appointment appointment) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.ClinicalAppointment " +
            "(pet_id,professional_id,reason,starts_at,ends_at,status," +
            "specialty,consultation_id,created_at,reminder_sent) " +
            "OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,?,?)",
            Long.class,
            appointment.petId(), appointment.professionalId(),
            appointment.reason(), appointment.startsAt(),
            appointment.endsAt(), appointment.status(),
            appointment.specialty(), appointment.consultationId(),
            appointment.createdAt(), appointment.reminderSent()
        );

        return new Appointment(
            id, appointment.petId(), appointment.professionalId(),
            appointment.reason(), appointment.startsAt(),
            appointment.endsAt(), appointment.status(),
            appointment.specialty(), appointment.consultationId(),
            appointment.createdAt(), appointment.reminderSent()
        );
    }

    public Appointment findById(long id) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalAppointment WHERE id=?",
            SqlClinicalMappers.APPOINTMENT, id
        ).stream().findFirst().orElse(null);
    }

    public List<Appointment> findAvailability(
        long professionalId,
        LocalDateTime start,
        LocalDateTime end
    ) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalAppointment " +
            "WHERE professional_id=? AND status='CONFIRMED' " +
            "AND starts_at<? AND ends_at>? ORDER BY starts_at",
            SqlClinicalMappers.APPOINTMENT,
            professionalId, end, start
        );
    }

    public boolean hasConflict(
        long professionalId,
        LocalDateTime start,
        LocalDateTime end,
        long excludedId
    ) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalAppointment " +
            "WITH (UPDLOCK,HOLDLOCK) " +
            "WHERE professional_id=? AND id<>? " +
            "AND status='CONFIRMED' AND starts_at<? AND ends_at>?",
            Integer.class,
            professionalId, excludedId, end, start
        );

        return count != null && count > 0;
    }

    public void reschedule(
        long id,
        LocalDateTime start,
        LocalDateTime end
    ) {
        int affected = jdbc.update(
            "UPDATE dbo.ClinicalAppointment SET " +
            "starts_at=?,ends_at=?,reminder_sent=0 " +
            "WHERE id=? AND status='CONFIRMED'",
            start, end, id
        );

        if (affected != 1) {
            throw new IllegalStateException(
                "No se pudo reprogramar"
            );
        }
    }

    public void cancel(long id) {
        int affected = jdbc.update(
            "UPDATE dbo.ClinicalAppointment SET status='CANCELLED' " +
            "WHERE id=? AND status='CONFIRMED'",
            id
        );

        if (affected != 1) {
            throw new IllegalStateException(
                "No se pudo cancelar"
            );
        }
    }

    public List<Appointment> findReminderCandidates(
        LocalDateTime now,
        LocalDateTime until
    ) {
        return jdbc.query(
            "SELECT * FROM dbo.ClinicalAppointment " +
            "WHERE status='CONFIRMED' AND reminder_sent=0 " +
            "AND starts_at>? AND starts_at<=? " +
            "AND DATEDIFF(MINUTE,created_at,starts_at)>=1440 " +
            "ORDER BY starts_at",
            SqlClinicalMappers.APPOINTMENT, now, until
        );
    }

    public void markReminderSent(long id) {
        jdbc.update(
            "UPDATE dbo.ClinicalAppointment SET reminder_sent=1 " +
            "WHERE id=? AND status='CONFIRMED' AND reminder_sent=0",
            id
        );
    }
}
