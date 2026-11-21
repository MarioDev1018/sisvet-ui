package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.FollowUpRequest;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.FollowUpUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class FollowUpService implements FollowUpUseCase {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final FollowUpRepositoryPort followUps;
    private final AppointmentRepositoryPort appointments;
    private final PatientRepositoryPort patients;
    private final ClinicalRecordPort records;
    private final NotificationDispatchPort notifications;
    private final ActorPort actors;

    public FollowUpService(
        FollowUpRepositoryPort followUps,
        AppointmentRepositoryPort appointments,
        PatientRepositoryPort patients,
        ClinicalRecordPort records,
        NotificationDispatchPort notifications,
        ActorPort actors
    ) {
        this.followUps = followUps;
        this.appointments = appointments;
        this.patients = patients;
        this.records = records;
        this.notifications = notifications;
        this.actors = actors;
    }

    @Transactional
    public FollowUp scheduleFollowUp(FollowUpRequest r) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (r == null || patients.findById(r.petId()) == null ||
            r.dueAt() == null ||
            r.reason() == null || r.reason().isBlank()) {
            throw new IllegalArgumentException(
                "Seguimiento inválido"
            );
        }

        if (r.consultationId() != null) {
            MedicalConsultation consultation =
                records.findConsultation(r.consultationId());

            if (consultation == null ||
                consultation.petId() != r.petId()) {
                throw new IllegalArgumentException(
                    "Referencia clínica incorrecta"
                );
            }
        }

        return followUps.save(
            new FollowUp(
                0, r.petId(), r.consultationId(), r.reason(),
                r.dueAt(), "PENDING", null, r.appointmentId()
            )
        );
    }

    public List<FollowUp> getPendingFollowUps() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        return followUps.findOutstanding();
    }

    public void reviewPendingFollowUps() {
        LocalDateTime now = LocalDateTime.now(LIMA_ZONE);

        for (FollowUp followUp : followUps.findPending(now)) {
            Pet pet = patients.findById(followUp.petId());

            if (pet != null && notifications.send(
                pet.ownerId(),
                "FOLLOWUP-" + followUp.id(),
                "Seguimiento clínico",
                followUp.reason()
            )) {
                followUps.markReviewed(followUp.id());
            }
        }

        for (Appointment appointment :
            appointments.findReminderCandidates(now, now.plusDays(1))) {

            Pet pet = patients.findById(appointment.petId());

            if (pet != null && notifications.send(
                pet.ownerId(),
                "APPOINTMENT-" + appointment.id(),
                "Recordatorio de cita",
                appointment.reason()
            )) {
                appointments.markReminderSent(appointment.id());
            }
        }
    }

    @Transactional
    public void rescheduleAppointmentFollowUp(
        long appointmentId,
        LocalDateTime date
    ) {
        followUps.rescheduleByAppointment(appointmentId, date);
    }

    @Transactional
    public void cancelAppointmentFollowUp(long appointmentId) {
        followUps.cancelByAppointment(appointmentId);
    }

    @Transactional
    public void completeFollowUp(long id) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        FollowUp followUp = followUps.findById(id);

        if (followUp == null) {
            throw new IllegalArgumentException(
                "Seguimiento inexistente"
            );
        }

        followUps.complete(id);
    }

    @Transactional
    public String notifyFollowUpNow(long id) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        FollowUp followUp = followUps.findById(id);
        if (followUp == null) {
            throw new IllegalArgumentException("Seguimiento inexistente");
        }

        Pet pet = patients.findById(followUp.petId());
        if (pet == null) {
            throw new IllegalArgumentException("Mascota inexistente");
        }

        String subject = "Seguimiento clínico para " + pet.name();
        String body = "Le recordamos el seguimiento programado para su mascota " + pet.name()
            + ": " + followUp.reason() + " (fecha programada: " + followUp.dueAt().toLocalDate() + ").";

        String detail = notifications.sendWithDetail(
            pet.ownerId(),
            "FOLLOWUP-MANUAL-" + followUp.id() + "-" + System.currentTimeMillis(),
            subject,
            body
        );

        followUps.markReviewed(followUp.id());
        return detail;
    }
}
