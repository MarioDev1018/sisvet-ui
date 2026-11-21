package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.ClinicalQueryUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class ClinicalQueryService implements ClinicalQueryUseCase {

    private final ClinicalQueryPort queries;
    private final ProfessionalLookupPort professionals;
    private final ActorPort actors;

    public ClinicalQueryService(
        ClinicalQueryPort queries,
        ProfessionalLookupPort professionals,
        ActorPort actors
    ) {
        this.queries = queries;
        this.professionals = professionals;
        this.actors = actors;
    }

    public List<PetSummary> searchPetSummaries(String criteria) {
        requireAnyClinicalStaff();
        return queries.searchPetSummaries(criteria == null ? "" : criteria.trim());
    }

    public PetSummary findPetSummary(long petId) {
        requireAnyClinicalStaff();
        PetSummary pet = queries.findPetSummary(petId);
        if (pet == null) {
            throw new IllegalArgumentException("Mascota no encontrada");
        }
        return pet;
    }

    public MedicalHistoryView getMedicalHistoryView(long petId) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );
        MedicalHistoryView view = queries.buildMedicalHistoryView(petId);
        if (view == null) {
            throw new IllegalArgumentException("Mascota no encontrada");
        }
        return view;
    }

    public List<AttentionSummary> listActiveAttentions() {
        requireAnyClinicalStaff();
        return queries.findActiveAttentions();
    }

    public List<ProfessionalOption> listActiveProfessionals() {
        requireAnyClinicalStaff();
        return queries.listActiveProfessionals();
    }

    @Transactional
    public void assignSpecialty(long professionalId, String specialty) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );
        if (professionalId <= 0 || specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Profesional y especialidad son obligatorios");
        }
        if (!professionals.canAttend(professionalId)) {
            throw new IllegalArgumentException("El usuario no es un profesional clínico activo");
        }
        professionals.assignSpecialty(professionalId, specialty.trim());
    }

    @Transactional
    public void removeSpecialty(long professionalId, String specialty) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );
        if (professionalId <= 0 || specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Profesional y especialidad son obligatorios");
        }
        professionals.removeSpecialty(professionalId, specialty.trim());
    }

    public List<AppointmentView> listAppointmentViews(Long professionalId, LocalDateTime start, LocalDateTime end) {
        requireAnyClinicalStaff();
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Rango de consulta inválido");
        }
        return queries.findAppointmentViews(professionalId, start, end);
    }

    public List<FollowUpView> listPendingFollowUpViews() {
        requireAnyClinicalStaff();
        return queries.findOutstandingFollowUpViews();
    }

    public ConsultationDetailView getConsultationDetail(long consultationId) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );
        ConsultationDetailView view = queries.findConsultationDetail(consultationId);
        if (view == null) {
            throw new IllegalArgumentException("Consulta clínica no encontrada");
        }
        return view;
    }

    public List<ConsultationDetailView> listOpenConsultations() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "VETERINARIO",
            "VETERINARIAN", "ESPECIALISTA"
        );
        return queries.findOpenConsultationDetails();
    }

    private void requireAnyClinicalStaff() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
    }
}
