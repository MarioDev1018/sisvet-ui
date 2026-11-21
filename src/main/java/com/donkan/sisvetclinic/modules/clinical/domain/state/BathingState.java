package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class BathingState implements AttentionState {

    public String getName() {
        return "EN_BANO";
    }

    public void advance(ClinicalAttention attention) {
        attention.changeState(new GroomingState());
    }
}
