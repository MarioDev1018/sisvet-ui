package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class WaitingState implements AttentionState {

    public String getName() {
        return "EN_ESPERA";
    }

    public void advance(ClinicalAttention attention) {
        if (attention.getArrivedAt() == null) {
            throw new IllegalStateException(
                "Primero debe registrarse la llegada"
            );
        }

        attention.changeState(new InProgressState());
    }
}
