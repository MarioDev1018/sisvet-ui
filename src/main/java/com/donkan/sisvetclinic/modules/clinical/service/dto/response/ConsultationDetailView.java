package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.donkan.sisvetclinic.modules.clinical.domain.*;

public record ConsultationDetailView(
    long id,
    long attentionId,
    long petId,
    String petName,
    String species,
    String speciesLabel,
    long ownerId,
    String ownerName,
    long veterinarianId,
    String veterinarianName,
    String reason,
    BigDecimal weight,
    String symptoms,
    String observations,
    String status,
    LocalDateTime createdAt,
    List<Diagnosis> diagnoses,
    List<Treatment> treatments,
    List<ClinicalProcedure> procedures,
    List<Vaccination> vaccinations,
    List<Referral> referrals
) {}
