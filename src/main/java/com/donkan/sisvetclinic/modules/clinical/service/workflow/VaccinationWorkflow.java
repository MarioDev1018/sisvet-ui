package com.donkan.sisvetclinic.modules.clinical.service.workflow;

import org.springframework.stereotype.Component;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;

@Component
public class VaccinationWorkflow extends AttentionWorkflow {

    public boolean supports(String type) {
        return "VACCINATION".equalsIgnoreCase(type);
    }

    protected void perform(ClinicalAttention attention) {
        attention.advance();
    }
}
