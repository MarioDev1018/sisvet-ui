package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.FollowUp;

public interface FollowUpRepositoryPort {

    FollowUp save(FollowUp followUp);

    FollowUp findById(long id);

    List<FollowUp> findPending(LocalDateTime until);

    List<FollowUp> findOutstanding();

    List<FollowUp> listByPet(long petId);

    void markReviewed(long id);

    void complete(long id);

    void rescheduleByAppointment(
        long appointmentId,
        LocalDateTime date
    );

    void cancelByAppointment(long appointmentId);
}
