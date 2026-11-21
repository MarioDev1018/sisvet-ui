package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.math.BigDecimal;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.SpeciesOption;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightPolicyView;

public interface WeightPolicyPort {

    String warning(String species, BigDecimal weight);

    WeightPolicyView findForSpecies(String species);

    List<WeightPolicyView> findAll();

    List<SpeciesOption> findRegisteredSpecies();

    void configure(
        String species,
        BigDecimal minimum,
        BigDecimal maximum
    );
}
