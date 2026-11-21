package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class ReadyForPickupState implements AttentionState {

    public String getName() { return "LISTO_RECOJO"; }

    public void advance(ClinicalAttention attention) {
        attention.assertServiceCompleted();
        attention.changeState(new PickedUpState());
    }
}
