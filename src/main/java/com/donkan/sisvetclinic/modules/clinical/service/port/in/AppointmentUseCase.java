package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.Appointment;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;

public interface AppointmentUseCase {

    List<Appointment> getAvailability(AvailabilityFilter filter);

    Appointment scheduleAppointment(AppointmentRequest request);

    Appointment scheduleFollowUp(AppointmentRequest request);

    Appointment scheduleSpecialist(AppointmentRequest request);

    void rescheduleAppointment(long id, java.time.LocalDateTime date);

    void cancelAppointment(long id);
}
