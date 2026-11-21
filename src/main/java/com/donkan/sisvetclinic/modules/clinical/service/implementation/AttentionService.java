package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.AttentionUseCase;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;
import com.donkan.sisvetclinic.modules.clinical.service.workflow.AttentionWorkflow;

@Service
public class AttentionService implements AttentionUseCase {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final AttentionRepositoryPort attentions;
    private final PatientRepositoryPort patients;
    private final ClinicalRecordPort records;
    private final ClinicalChargePort charges;
    private final ActorPort actors;
    private final List<AttentionWorkflow> workflows;

    public AttentionService(
        AttentionRepositoryPort attentions,
        PatientRepositoryPort patients,
        ClinicalRecordPort records,
        ClinicalChargePort charges,
        ActorPort actors,
        List<AttentionWorkflow> workflows
    ) {
        this.attentions = attentions;
        this.patients = patients;
        this.records = records;
        this.charges = charges;
        this.actors = actors;
        this.workflows = workflows;
    }

    @Transactional
    public ClinicalAttention registerArrival(ArrivalRequest r) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        if (r == null || patients.findById(r.petId()) == null) {
            throw new IllegalArgumentException("Paciente no encontrado");
        }

        String type = r.type() == null ? "MEDICAL" :
            r.type().trim().toUpperCase(Locale.ROOT);

        if (!"MEDICAL".equals(type) && !"VACCINATION".equals(type)) {
            throw new IllegalArgumentException(
                "Solo se admiten MEDICAL y VACCINATION en esta versión"
            );
        }

        if ("VACCINATION".equals(type) && r.requiresGrooming()) {
            throw new IllegalArgumentException(
                "La vacunación independiente no incluye peluquería"
            );
        }

        ClinicalAttention attention = attentions.findOpenByPet(r.petId());

        if (attention != null && attention.getArrivedAt() != null) {
            if ("URGENCY".equals(attention.getType()) && "EN_ESPERA".equals(attention.getStatus())) {
                return attention;
            }
            throw new IllegalStateException(
                "La mascota conserva la atención #" + attention.getId() +
                " en estado " + attention.getStatus() +
                ". Debe resolver esa atención antes de registrar otra llegada"
            );
        }

        if (attention != null && !"EN_ESPERA".equals(attention.getStatus())) {
            throw new IllegalStateException("Atención previa pendiente de regularización");
        }

        if (attention == null) {
            attention = new ClinicalAttention(
                0, r.petId(), type, null, null, null,
                "EN_ESPERA", null, r.requiresGrooming(), false, null
            );
        } else if (!"URGENCY".equals(attention.getType()) &&
                   !type.equals(attention.getType())) {
            throw new IllegalStateException(
                "Existe una atención previa con otro tipo: " + attention.getType()
            );
        }

        attention.markArrived(LocalDateTime.now(LIMA_ZONE), r.requiresGrooming());
        return attentions.save(attention);
    }

    @Transactional
    public void registerUrgency(UrgencyRequest r) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN"
        );

        if (r == null || patients.findById(r.petId()) == null) {
            throw new IllegalArgumentException("Mascota inexistente");
        }

        ClinicalAttention attention = attentions.findOpenByPet(r.petId());

        if (attention == null) {
            attention = new ClinicalAttention(
                0, r.petId(), "URGENCY", null, null, null,
                "EN_ESPERA", null, false, false, null
            );
        }

        if (!"EN_ESPERA".equals(attention.getStatus())) {
            throw new IllegalStateException(
                "La urgencia debe registrarse durante la espera"
            );
        }

        if (attention.getArrivedAt() == null) {
            attention.markArrived(LocalDateTime.now(LIMA_ZONE), attention.isRequiresGrooming());
        }

        attention.registerUrgency(r.description());
        attentions.save(attention);
    }

    @Transactional
    public void assignPriority(long id, int priority) {
        Actor actor = ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        ClinicalAttention attention = requireAttention(id);
        attention.assignPriority(priority, actor.id());
        attentions.save(attention);
        attentions.auditPriority(id, actor.id(), priority);
    }

    public List<ClinicalAttention> getWaitingPatients() {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
        return attentions.findWaiting();
    }

    @Transactional
    public void startAttention(long id) {
        ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );

        ClinicalAttention attention = requireAttention(id);
        String type = "URGENCY".equals(attention.getType())
            ? "MEDICAL" : attention.getType();

        AttentionWorkflow workflow = workflows.stream()
            .filter(w -> w.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No existe workflow para " + type
            ));

        workflow.execute(attention);
        attentions.save(attention);
    }

    @Transactional
    public void advanceAttention(long id) {
        ClinicalAttention attention = requireAttention(id);

        if ("EN_BANO".equals(attention.getStatus()) ||
            "EN_PELUQUERIA".equals(attention.getStatus())) {
            ClinicalAccess.require(
                actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA",
                "RECEPCIONISTA", "RECEPTIONIST", "ADMINISTRADOR", "ADMIN"
            );
        } else {
            ClinicalAccess.require(
                actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
            );
        }

        if ("LISTO_RECOJO".equals(attention.getStatus()) ||
            "ATENDIDO".equals(attention.getStatus())) {
            throw new IllegalStateException(
                "Use el registro de retiro para finalizar la atención"
            );
        }

        attention.advance();
        attentions.save(attention);
    }

    @Transactional
    public void registerPickup(long id) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA", "RECEPTIONIST"
        );

        ClinicalAttention attention = requireAttention(id);

        if (!"LISTO_RECOJO".equals(attention.getStatus())) {
            throw new IllegalStateException(
                "La mascota todavía no está lista para recojo"
            );
        }

        attention.assertServiceCompleted();

        if (attention.requiresMedicalConsultation()) {
            if (!charges.isSettled(attention.getConsultationId())) {
                throw new IllegalStateException(
                    "La atención clínica registra cobros pendientes"
                );
            }
        } else if ("VACCINATION".equals(attention.getType())) {
            if (!charges.isSettledByAttention(attention.getId())) {
                throw new IllegalStateException(
                    "La vacunación registra cobros pendientes"
                );
            }
        }

        // La vacunación sin consulta se cierra sin inventar una consulta médica.
        // El cobro de una vacunación independiente requiere su vínculo comercial propio.
        attention.advance();
        attentions.save(attention);
    }

    private ClinicalAttention requireAttention(long id) {
        ClinicalAttention attention = attentions.findById(id);

        if (attention == null) {
            throw new IllegalArgumentException("Atención no encontrada");
        }

        return attention;
    }
}
