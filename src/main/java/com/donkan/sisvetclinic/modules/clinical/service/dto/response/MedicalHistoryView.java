package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.*;

public record MedicalHistoryView(
    PetSummary pet,
    List<WeightPoint> weightHistory,
    List<MedicalConsultation> consultations,
    List<Diagnosis> diagnoses,
    List<Treatment> treatments,
    List<ClinicalProcedure> procedures,
    List<Vaccination> vaccinations,
    List<Referral> referrals,
    List<AppointmentView> appointments,
    List<FollowUpView> followUps
) {
    public record WeightPoint(long consultationId, LocalDateTime recordedAt, BigDecimal weightKg, String reason) {}
}
