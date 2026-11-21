package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.Appointment;

public interface AppointmentRepositoryPort {

    Appointment save(Appointment appointment);

    Appointment findById(long id);

    List<Appointment> findAvailability(
        long professionalId,
        LocalDateTime start,
        LocalDateTime end
    );

    boolean hasConflict(
        long professionalId,
        LocalDateTime start,
        LocalDateTime end,
        long excludedId
    );

    void reschedule(
        long id,
        LocalDateTime start,
        LocalDateTime end
    );

    void cancel(long id);

    List<Appointment> findReminderCandidates(
        LocalDateTime now,
        LocalDateTime until
    );

    void markReminderSent(long id);
}
