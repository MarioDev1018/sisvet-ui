package com.donkan.sisvetclinic.modules.clinical.domain;

import com.donkan.sisvetclinic.modules.clinical.domain.state.*;
import java.time.LocalDateTime;

public class ClinicalAttention {

    private long id;
    private final long petId;
    private String type;
    private Integer priority;
    private String urgencyDescription;
    private Long priorityAssignedBy;
    private AttentionState state;
    private LocalDateTime arrivedAt;
    private boolean requiresGrooming;
    private boolean clinicalClosed;
    private Long consultationId;
    private boolean vaccinationCompleted;
    private long version;

    // Conserva compatibilidad con los constructores previos.
    public ClinicalAttention(
        long id, long petId, String type, Integer priority,
        String urgencyDescription, Long priorityAssignedBy, String state,
        LocalDateTime arrivedAt, boolean requiresGrooming,
        boolean clinicalClosed, Long consultationId
    ) {
        this(id, petId, type, priority, urgencyDescription, priorityAssignedBy,
            state, arrivedAt, requiresGrooming, clinicalClosed, consultationId,
            false, 0);
    }

    public ClinicalAttention(
        long id, long petId, String type, Integer priority,
        String urgencyDescription, Long priorityAssignedBy, String state,
        LocalDateTime arrivedAt, boolean requiresGrooming,
        boolean clinicalClosed, Long consultationId,
        boolean vaccinationCompleted, long version
    ) {
        this.id = id;
        this.petId = petId;
        this.type = type;
        this.priority = priority;
        this.urgencyDescription = urgencyDescription;
        this.priorityAssignedBy = priorityAssignedBy;
        this.state = AttentionStates.fromName(state);
        this.arrivedAt = arrivedAt;
        this.requiresGrooming = requiresGrooming;
        this.clinicalClosed = clinicalClosed;
        this.consultationId = consultationId;
        this.vaccinationCompleted = vaccinationCompleted;
        this.version = version;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPetId() { return petId; }
    public String getType() { return type; }
    public Integer getPriority() { return priority; }
    public String getUrgencyDescription() { return urgencyDescription; }
    public Long getPriorityAssignedBy() { return priorityAssignedBy; }
    public String getStatus() { return state.getName(); }
    public LocalDateTime getArrivedAt() { return arrivedAt; }
    public boolean isRequiresGrooming() { return requiresGrooming; }
    public boolean isClinicalClosed() { return clinicalClosed; }
    public Long getConsultationId() { return consultationId; }
    public boolean isVaccinationCompleted() { return vaccinationCompleted; }
    public long getVersion() { return version; }

    public void incrementVersion() { version++; }

    public boolean requiresMedicalConsultation() {
        return "MEDICAL".equals(type) || "URGENCY".equals(type);
    }

    public void assertServiceCompleted() {
        if (requiresMedicalConsultation()) {
            if (!clinicalClosed || consultationId == null) {
                throw new IllegalStateException(
                    "Falta cerrar la consulta con diagnóstico e indicaciones"
                );
            }
        } else if ("VACCINATION".equals(type)) {
            if (!vaccinationCompleted) {
                throw new IllegalStateException(
                    "Debe registrar la vacuna aplicada antes de finalizar"
                );
            }
        } else {
            throw new IllegalStateException(
                "El flujo de atención no está habilitado: " + type
            );
        }
    }

    public void markVaccinationCompleted() {
        if (!"VACCINATION".equals(type) ||
            !"ATENDIENDOSE".equals(getStatus()) || arrivedAt == null) {
            throw new IllegalStateException(
                "La vacunación debe estar en una atención iniciada"
            );
        }

        vaccinationCompleted = true;
    }

    public void markArrived(LocalDateTime at, boolean grooming) {
        if (arrivedAt != null) {
            throw new IllegalStateException("La llegada ya fue registrada");
        }
        if (!"EN_ESPERA".equals(getStatus())) {
            throw new IllegalStateException("La atención ya no está en espera");
        }
        arrivedAt = at;
        requiresGrooming = grooming;
    }

    public void registerUrgency(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Describa la urgencia");
        }
        if (priority != null) {
            throw new IllegalStateException(
                "Solo el veterinario puede modificar la valoración clínica"
            );
        }
        urgencyDescription = description;
        type = "URGENCY";
    }

    public void assignPriority(int value, long veterinarianId) {
        if (urgencyDescription == null) {
            throw new IllegalStateException("No existe una urgencia registrada");
        }
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Prioridad entre 1 y 5");
        }
        priority = value;
        priorityAssignedBy = veterinarianId;
    }

    public void setConsultationId(long id) { consultationId = id; }
    public void setClinicalClosed() { clinicalClosed = true; }
    public void changeState(AttentionState newState) { state = newState; }
    public void advance() { state.advance(this); }
}
