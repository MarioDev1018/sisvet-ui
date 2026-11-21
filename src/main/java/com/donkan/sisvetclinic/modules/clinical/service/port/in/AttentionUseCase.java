package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;

public interface AttentionUseCase {

    ClinicalAttention registerArrival(ArrivalRequest request);

    void registerUrgency(UrgencyRequest request);

    void assignPriority(long id, int priority);

    List<ClinicalAttention> getWaitingPatients();

    void startAttention(long id);

    void advanceAttention(long id);

    void registerPickup(long id);
}
