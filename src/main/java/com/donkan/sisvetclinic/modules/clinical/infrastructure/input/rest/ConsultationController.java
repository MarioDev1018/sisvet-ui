package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;

@RestController
@RequestMapping("/api/clinical/consultations")
public class ConsultationController {

    private final ConsultationUseCase consultations;
    private final ClinicalQueryUseCase queries;

    public ConsultationController(ConsultationUseCase consultations) {
        this(consultations, null);
    }

    @Autowired
    public ConsultationController(ConsultationUseCase consultations, ClinicalQueryUseCase queries) {
        this.consultations = consultations;
        this.queries = queries;
    }

    @GetMapping("/open")
    public List<ConsultationDetailView> open() {
        return queries.listOpenConsultations();
    }

    @GetMapping("/{id}")
    public ConsultationDetailView detail(@PathVariable long id) {
        return queries.getConsultationDetail(id);
    }

    @PostMapping
    public MedicalConsultation register(
        @RequestBody ConsultationRequest request
    ) {
        return consultations.registerConsultation(request);
    }

    @PostMapping("/diagnoses")
    public Diagnosis diagnose(@RequestBody DiagnosisRequest request) {
        return consultations.registerDiagnosis(request);
    }

    @PutMapping("/{id}/complete")
    public void complete(@PathVariable long id) {
        consultations.completeConsultation(id);
    }

    @GetMapping("/weight-warning")
    public String weight(
        @RequestParam long petId,
        @RequestParam BigDecimal weight
    ) {
        return consultations.checkWeight(petId, weight);
    }

    @GetMapping("/weight-evaluation")
    public WeightCheckResult evaluateWeight(
        @RequestParam long petId,
        @RequestParam BigDecimal weight
    ) {
        return consultations.evaluateWeight(petId, weight);
    }
}
