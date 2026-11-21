package com.donkan.sisvetclinic.modules.clinical.domain.state;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public final class PickedUpState implements AttentionState {

    public String getName() {
        return "ATENDIDO";
    }

    public void advance(ClinicalAttention attention) {
        throw new IllegalStateException(
            "La atención ya ha concluido"
        );
    }
}
