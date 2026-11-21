package com.donkan.sisvetclinic.modules.clinical.service.workflow;

import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

public abstract class AttentionWorkflow {

    public final void execute(ClinicalAttention attention) {
        validate(attention);
        perform(attention);
    }

    protected void validate(ClinicalAttention attention) {
        if (attention.getArrivedAt() == null || !"EN_ESPERA".equals(attention.getStatus())) {
            throw new IllegalStateException(
                "La mascota debe registrar su llegada antes de iniciar atención"
            );
        }
    }

    protected abstract void perform(ClinicalAttention attention);

    public abstract boolean supports(String type);
}
