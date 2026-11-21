package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public interface AttentionState {
    void advance(ClinicalAttention attention);
    String getName();
}
