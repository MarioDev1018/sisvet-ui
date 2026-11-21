package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.WeightRangeRequest;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.SpeciesOption;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightPolicyView;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.WeightPolicyUseCase;

@RestController
@RequestMapping("/api/clinical/weight-policies")
public class ClinicalWeightPolicyController {

    private final WeightPolicyUseCase policies;

    public ClinicalWeightPolicyController(
        WeightPolicyUseCase policies
    ) {
        this.policies = policies;
    }

    @GetMapping
    public List<WeightPolicyView> list() {
        return policies.listPolicies();
    }

    @GetMapping("/species")
    public List<SpeciesOption> listSpecies() {
        return policies.listSpecies();
    }

    @PutMapping
    public void configure(@RequestBody WeightRangeRequest request) {
        policies.configure(request);
    }
}
