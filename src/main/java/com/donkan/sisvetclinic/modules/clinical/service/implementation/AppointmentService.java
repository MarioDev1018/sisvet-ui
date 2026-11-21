package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class AppointmentService implements AppointmentUseCase {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final AppointmentRepositoryPort appointments;
    private final PatientRepositoryPort patients;
    private final ClinicalRecordPort records;
    private final FollowUpUseCase followUps;
    private final ActorPort actors;
    private final ProfessionalLookupPort professionals;

    public AppointmentService(
        AppointmentRepositoryPort appointments,
        PatientRepositoryPort patients,
        ClinicalRecordPort records,
        FollowUpUseCase followUps,
        ActorPort actors,
        ProfessionalLookupPort professionals
    ) {
        this.appointments = appointments;
        this.patients = patients;
        this.records = records;
        this.followUps = followUps;
        this.actors = actors;
        this.professionals = professionals;
    }

    public List<Appointment> getAvailability(AvailabilityFilter f) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (f == null || f.start() == null || f.end() == null ||
            !f.end().isAfter(f.start())) {
            throw new IllegalArgumentException(
                "Rango de consulta inválido"
            );
        }

        return appointments.findAvailability(
            f.professionalId(), f.start(), f.end()
        );
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment scheduleAppointment(AppointmentRequest request) {
        return create(request, "GENERAL");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment scheduleFollowUp(AppointmentRequest request) {
        Appointment appointment = create(request, "FOLLOW_UP");

        followUps.scheduleFollowUp(
            new FollowUpRequest(
                appointment.petId(), appointment.consultationId(),
                "Cita de seguimiento: " + appointment.reason(),
                appointment.startsAt(), appointment.id()
            )
        );

        return appointment;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment scheduleSpecialist(AppointmentRequest request) {
        if (request == null ||
            request.specialty() == null ||
            request.specialty().isBlank()) {
            throw new IllegalArgumentException(
                "La especialidad es obligatoria"
            );
        }

        return create(request, "SPECIALIST");
    }

    private Appointment create(AppointmentRequest r, String type) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (r == null || patients.findById(r.petId()) == null ||
            r.professionalId() <= 0 || r.date() == null ||
            r.reason() == null || r.reason().isBlank()) {
            throw new IllegalArgumentException(
                "Datos de cita incompletos"
            );
        }

        if (!professionals.canAttend(r.professionalId())) {
            throw new IllegalArgumentException(
                "El usuario no es un profesional clínico activo"
            );
        }

        if ("SPECIALIST".equals(type) &&
            !professionals.hasSpecialty(r.professionalId(), r.specialty())) {
            throw new IllegalArgumentException(
                "El profesional no tiene la especialidad seleccionada"
            );
        }

        int duration = r.durationMinutes() == 0
            ? 30 : r.durationMinutes();

        LocalDateTime now = LocalDateTime.now(LIMA_ZONE);

        if (duration < 15 || duration > 180 ||
            !r.date().isAfter(now)) {
            throw new IllegalArgumentException(
                "Horario o duración inválidos"
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

        LocalDateTime end = r.date().plusMinutes(duration);

        if (appointments.hasConflict(
            r.professionalId(), r.date(), end, 0
        )) {
            throw new IllegalStateException(
                "El profesional ya tiene cita en ese horario"
            );
        }

        return appointments.save(
            new Appointment(
                0, r.petId(), r.professionalId(),
                type + ": " + r.reason(),
                r.date(), end, "CONFIRMED",
                r.specialty(), r.consultationId(),
                now, false
            )
        );
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void rescheduleAppointment(long id, LocalDateTime date) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        Appointment appointment = appointments.findById(id);

        if (appointment == null ||
            !"CONFIRMED".equals(appointment.status()) ||
            date == null || !date.isAfter(LocalDateTime.now(LIMA_ZONE))) {
            throw new IllegalStateException(
                "La cita no puede reprogramarse"
            );
        }

        LocalDateTime end = date.plus(
            Duration.between(
                appointment.startsAt(), appointment.endsAt()
            )
        );

        if (appointments.hasConflict(
            appointment.professionalId(), date, end, id
        )) {
            throw new IllegalStateException(
                "El nuevo horario está ocupado"
            );
        }

        appointments.reschedule(id, date, end);
        followUps.rescheduleAppointmentFollowUp(id, date);
    }

    @Transactional
    public void cancelAppointment(long id) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        Appointment appointment = appointments.findById(id);

        if (appointment == null ||
            !"CONFIRMED".equals(appointment.status())) {
            throw new IllegalStateException(
                "La cita no puede cancelarse"
            );
        }

        appointments.cancel(id);
        followUps.cancelAppointmentFollowUp(id);
    }
}
