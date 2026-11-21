package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class InProgressState implements AttentionState {

    public String getName() { return "ATENDIENDOSE"; }

    public void advance(ClinicalAttention attention) {
        attention.assertServiceCompleted();

        if (attention.requiresMedicalConsultation() &&
            attention.isRequiresGrooming()) {
            attention.changeState(new BathingState());
        } else {
            attention.changeState(new ReadyForPickupState());
        }
    }
}
