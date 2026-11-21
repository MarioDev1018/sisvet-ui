package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.MedicationDetail;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.VaccineProductOption;

public interface MedicationLookupPort {
    List<MedicationDetail> search(String name);

    default List<VaccineProductOption> searchVaccines(String name) {
        return List.of();
    }
}
