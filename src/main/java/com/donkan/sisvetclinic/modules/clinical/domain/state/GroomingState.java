package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class GroomingState implements AttentionState {

    public String getName() {
        return "EN_PELUQUERIA";
    }

    public void advance(ClinicalAttention attention) {
        attention.changeState(new ReadyForPickupState());
    }
}
