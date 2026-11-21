package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.ConsultationUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Service
public class ConsultationService implements ConsultationUseCase {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final BigDecimal DEFAULT_CONSULTATION_FEE = new BigDecimal("50.00");

    private final ClinicalRecordPort records;
    private final AttentionRepositoryPort attentions;
    private final PatientRepositoryPort patients;
    private final WeightPolicyPort weightPolicy;
    private final ActorPort actors;
    private final ClinicalChargePort charges;

    public ConsultationService(
        ClinicalRecordPort records,
        AttentionRepositoryPort attentions,
        PatientRepositoryPort patients,
        WeightPolicyPort weightPolicy,
        ActorPort actors
    ) {
        this(records, attentions, patients, weightPolicy, actors, null);
    }

    @Autowired
    public ConsultationService(
        ClinicalRecordPort records,
        AttentionRepositoryPort attentions,
        PatientRepositoryPort patients,
        WeightPolicyPort weightPolicy,
        ActorPort actors,
        ClinicalChargePort charges
    ) {
        this.records = records;
        this.attentions = attentions;
        this.patients = patients;
        this.weightPolicy = weightPolicy;
        this.actors = actors;
        this.charges = charges;
    }

    @Transactional
    public MedicalConsultation registerConsultation(ConsultationRequest r) {
        Actor actor = ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (r == null || r.reason() == null || r.reason().isBlank() ||
            r.weight() == null || r.weight().signum() <= 0) {
            throw new IllegalArgumentException(
                "Motivo y peso válido son obligatorios"
            );
        }

        ClinicalAttention attention = requireAttention(r.attentionId());

        if (!"ATENDIENDOSE".equals(attention.getStatus())) {
            throw new IllegalStateException("La atención no está iniciada");
        }

        if (attention.getConsultationId() != null) {
            throw new IllegalStateException(
                "La atención ya tiene consulta"
            );
        }

        MedicalConsultation saved = records.saveConsultation(
            new MedicalConsultation(
                0, attention.getId(), attention.getPetId(), actor.id(),
                r.reason(), r.weight(), r.symptoms(),
                r.observations(), "OPEN", LocalDateTime.now(LIMA_ZONE)
            )
        );

        attention.setConsultationId(saved.id());
        attentions.save(attention);

        if (charges != null) {
            charges.registerCharge(
                saved.id(),
                "Consulta clínica: " + r.reason().trim(),
                DEFAULT_CONSULTATION_FEE
            );
        }

        return saved;
    }

    @Transactional
    public Diagnosis registerDiagnosis(DiagnosisRequest r) {
        Actor actor = ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        if (r == null || r.description() == null || r.description().isBlank() ||
            r.indications() == null || r.indications().isBlank()) {
            throw new IllegalArgumentException(
                "Diagnóstico e indicaciones son obligatorios"
            );
        }

        MedicalConsultation consultation =
            records.findConsultation(r.consultationId());

        if (consultation == null || "COMPLETED".equals(consultation.status())) {
            throw new IllegalStateException(
                "Consulta inexistente o finalizada"
            );
        }

        return records.saveDiagnosis(
            new Diagnosis(
                0, r.consultationId(), r.description(),
                r.indications(), actor.id(), LocalDateTime.now(LIMA_ZONE)
            )
        );
    }

    @Transactional
    public void completeConsultation(long id) {
        Actor actor = ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        MedicalConsultation consultation = records.findConsultation(id);

        if (consultation == null ||
            "COMPLETED".equals(consultation.status())) {
            throw new IllegalStateException(
                "Consulta inválida para cierre"
            );
        }

        if (!records.hasCompleteDiagnosis(id)) {
            throw new IllegalStateException(
                "Debe registrar diagnóstico e indicaciones"
            );
        }

        records.completeConsultation(id, actor.id());

        ClinicalAttention attention =
            requireAttention(consultation.attentionId());

        attention.setClinicalClosed();
        attentions.save(attention);
    }

    public String checkWeight(long petId, BigDecimal weight) {
        ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        Pet pet = patients.findById(petId);

        if (pet == null) {
            throw new IllegalArgumentException("Mascota inexistente");
        }

        if (weight == null || weight.signum() <= 0) {
            throw new IllegalArgumentException("Peso debe ser positivo");
        }

        return weightPolicy.warning(pet.species(), weight);
    }

    public WeightCheckResult evaluateWeight(long petId, BigDecimal weight) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        Pet pet = patients.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("Mascota inexistente");
        }
        if (weight == null || weight.signum() <= 0) {
            throw new IllegalArgumentException("Peso debe ser positivo");
        }

        String canonical = ClinicalSpecies.canonicalCode(pet.species());
        String label = ClinicalSpecies.displayLabel(pet.species());
        WeightPolicyView policy = weightPolicy.findForSpecies(pet.species());
        List<MedicalConsultation> history = records.listByPet(petId);
        BigDecimal lastWeight = history.isEmpty() ? null : history.get(0).weight();

        StringBuilder msg = new StringBuilder();
        boolean configured = policy != null;
        boolean warning = false;

        if (!configured) {
            msg.append("No existe un rango de referencia configurado para ").append(label).append(". ");
        } else {
            boolean outOfRange = weight.compareTo(policy.minKg()) < 0
                    || weight.compareTo(policy.maxKg()) > 0;
            if (outOfRange) {
                warning = true;
                msg.append("Advertencia: peso fuera del rango orientativo configurado (")
                        .append(policy.minKg().stripTrailingZeros().toPlainString())
                        .append(" - ")
                        .append(policy.maxKg().stripTrailingZeros().toPlainString())
                        .append(" kg). ");
            } else {
                msg.append("Dentro del rango orientativo configurado (")
                        .append(policy.minKg().stripTrailingZeros().toPlainString())
                        .append(" - ")
                        .append(policy.maxKg().stripTrailingZeros().toPlainString())
                        .append(" kg). ");
            }
        }

        if (lastWeight != null && lastWeight.signum() > 0) {
            BigDecimal diff = weight.subtract(lastWeight).abs();
            BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                    .divide(lastWeight, 1, RoundingMode.HALF_UP);
            if (pct.compareTo(BigDecimal.valueOf(25)) >= 0) {
                warning = true;
                msg.append("Variación notable (").append(pct)
                        .append("%) respecto al último peso registrado (")
                        .append(lastWeight.stripTrailingZeros().toPlainString())
                        .append(" kg).");
            } else {
                msg.append("Último peso registrado: ")
                        .append(lastWeight.stripTrailingZeros().toPlainString())
                        .append(" kg.");
            }
        }

        return new WeightCheckResult(
                configured,
                warning,
                canonical,
                label,
                policy != null ? policy.minKg() : null,
                policy != null ? policy.maxKg() : null,
                lastWeight,
                msg.toString().trim()
        );
    }

    private ClinicalAttention requireAttention(long id) {
        ClinicalAttention attention = attentions.findById(id);

        if (attention == null) {
            throw new IllegalArgumentException("Atención inexistente");
        }

        return attention;
    }
}
