package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalSpecies;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.WeightRangeRequest;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.SpeciesOption;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.WeightPolicyView;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.WeightPolicyUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class WeightPolicyService implements WeightPolicyUseCase {

    private final WeightPolicyPort policies;
    private final ActorPort actors;

    public WeightPolicyService(
        WeightPolicyPort policies,
        ActorPort actors
    ) {
        this.policies = policies;
        this.actors = actors;
    }

    @Transactional
    public void configure(WeightRangeRequest r) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (r == null || r.species() == null ||
            r.species().isBlank() ||
            r.minimum() == null || r.maximum() == null ||
            r.minimum().signum() <= 0 ||
            r.maximum().compareTo(r.minimum()) < 0) {
            throw new IllegalArgumentException(
                "Rango de peso inválido"
            );
        }

        String canonical = ClinicalSpecies.canonicalCode(r.species());
        policies.configure(canonical, r.minimum(), r.maximum());
    }

    public List<WeightPolicyView> listPolicies() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
        return policies.findAll();
    }

    public List<SpeciesOption> listSpecies() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
        return policies.findRegisteredSpecies();
    }
}
