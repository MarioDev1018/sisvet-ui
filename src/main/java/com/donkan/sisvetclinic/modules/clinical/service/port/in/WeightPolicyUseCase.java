package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.WeightRangeRequest;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.SpeciesOption;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightPolicyView;

public interface WeightPolicyUseCase {
    void configure(WeightRangeRequest request);

    List<WeightPolicyView> listPolicies();

    List<SpeciesOption> listSpecies();
}
