package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;

public interface ClinicalQueryPort {

    List<PetSummary> searchPetSummaries(String criteria);

    PetSummary findPetSummary(long petId);

    List<AttentionSummary> findActiveAttentions();

    List<ProfessionalOption> listActiveProfessionals();

    List<AppointmentView> findAppointmentViews(Long professionalId, LocalDateTime start, LocalDateTime end);

    List<AppointmentView> listAppointmentsByPet(long petId);

    List<FollowUpView> findOutstandingFollowUpViews();

    List<FollowUpView> listFollowUpViewsByPet(long petId);

    ConsultationDetailView findConsultationDetail(long consultationId);

    List<ConsultationDetailView> findOpenConsultationDetails();

    MedicalHistoryView buildMedicalHistoryView(long petId);
}
