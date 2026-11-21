package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.rest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.donkan.sisvetclinic.modules.clinical.domain.ClinicalAttention;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.AttentionSummary;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;

@RestController
@RequestMapping("/api/clinical/attentions")
public class AttentionController {

    private final AttentionUseCase attentions;
    private final ClinicalQueryUseCase queries;

    public AttentionController(AttentionUseCase attentions) {
        this(attentions, null);
    }

    @Autowired
    public AttentionController(AttentionUseCase attentions, ClinicalQueryUseCase queries) {
        this.attentions = attentions;
        this.queries = queries;
    }

    @PostMapping("/arrivals")
    public ClinicalAttention arrival(@RequestBody ArrivalRequest request) {
        return attentions.registerArrival(request);
    }

    @PostMapping("/urgencies")
    public void urgency(@RequestBody UrgencyRequest request) {
        attentions.registerUrgency(request);
    }

    @PutMapping("/{id}/priority")
    public void priority(
        @PathVariable long id,
        @RequestBody PriorityRequest request
    ) {
        attentions.assignPriority(id, request.priority());
    }

    @GetMapping("/waiting")
    public List<ClinicalAttention> waiting() {
        return attentions.getWaitingPatients();
    }

    @GetMapping("/active")
    public List<AttentionSummary> active() {
        return queries.listActiveAttentions();
    }

    @PutMapping("/{id}/start")
    public void start(@PathVariable long id) {
        attentions.startAttention(id);
    }

    @PutMapping("/{id}/advance")
    public void advance(@PathVariable long id) {
        attentions.advanceAttention(id);
    }

    @PutMapping("/{id}/pickup")
    public void pickup(@PathVariable long id) {
        attentions.registerPickup(id);
    }
}
