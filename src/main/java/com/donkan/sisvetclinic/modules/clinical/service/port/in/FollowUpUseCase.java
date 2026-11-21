package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.FollowUp;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.FollowUpRequest;

public interface FollowUpUseCase {

    FollowUp scheduleFollowUp(FollowUpRequest request);

    List<FollowUp> getPendingFollowUps();

    void reviewPendingFollowUps();

    void completeFollowUp(long id);

    default String notifyFollowUpNow(long id) {
        return "Notificación enviada.";
    }

    void rescheduleAppointmentFollowUp(
        long appointmentId,
        java.time.LocalDateTime date
    );

    void cancelAppointmentFollowUp(long appointmentId);
}
