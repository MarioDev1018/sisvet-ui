package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public interface AttentionRepositoryPort {

    ClinicalAttention save(ClinicalAttention attention);

    ClinicalAttention findById(long id);

    ClinicalAttention findOpenByPet(long petId);

    List<ClinicalAttention> findWaiting();

    void auditPriority(
        long attentionId,
        long veterinarianId,
        int priority
    );
}
