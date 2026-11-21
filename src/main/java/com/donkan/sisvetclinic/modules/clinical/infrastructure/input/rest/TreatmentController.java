package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.VaccineProductOption;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.TreatmentUseCase;

@RestController
@RequestMapping("/api/clinical/treatments")
public class TreatmentController {

    private final TreatmentUseCase treatments;

    public TreatmentController(TreatmentUseCase treatments) {
        this.treatments = treatments;
    }

    @PostMapping
    public Treatment treatment(@RequestBody TreatmentRequest request) {
        return treatments.registerTreatment(request);
    }

    @PostMapping("/procedures")
    public ClinicalProcedure procedure(
        @RequestBody ProcedureRequest request
    ) {
        return treatments.registerProcedure(request);
    }

    @PostMapping("/vaccinations")
    public Vaccination vaccination(
        @RequestBody VaccinationRequest request
    ) {
        return treatments.registerVaccination(request);
    }

    @GetMapping("/medications")
    public List<MedicationDetail> medicines(
        @RequestParam(defaultValue = "") String search
    ) {
        return treatments.getMedicationOptions(search);
    }

    @GetMapping("/vaccines")
    public List<VaccineProductOption> vaccines(
        @RequestParam(defaultValue = "") String search
    ) {
        return treatments.getVaccineOptions(search);
    }

    @PostMapping("/referrals")
    public Referral referral(@RequestBody ReferralRequest request) {
        return treatments.registerReferral(request);
    }
}
