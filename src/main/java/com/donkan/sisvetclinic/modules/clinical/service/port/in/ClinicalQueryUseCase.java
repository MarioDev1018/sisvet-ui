package com.donkan.sisvetclinic.modules.clinical.service.port.in;

import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;

public interface ClinicalQueryUseCase {

    List<PetSummary> searchPetSummaries(String criteria);

    PetSummary findPetSummary(long petId);

    MedicalHistoryView getMedicalHistoryView(long petId);

    List<AttentionSummary> listActiveAttentions();

    List<ProfessionalOption> listActiveProfessionals();

    void assignSpecialty(long professionalId, String specialty);

    void removeSpecialty(long professionalId, String specialty);

    List<AppointmentView> listAppointmentViews(Long professionalId, LocalDateTime start, LocalDateTime end);

    List<FollowUpView> listPendingFollowUpViews();

    ConsultationDetailView getConsultationDetail(long consultationId);

    List<ConsultationDetailView> listOpenConsultations();
}
