package com.donkan.sisvetclinic.modules.clinical.domain;

import java.util.List;

public record MedicalHistory(
    long id,
    long petId,
    List<MedicalConsultation> consultations,
    List<Diagnosis> diagnoses,
    List<Treatment> treatments,
    List<ClinicalProcedure> procedures,
    List<Vaccination> vaccinations,
    List<Referral> referrals,
    List<FollowUp> followUps
) {}
