package com.donkan.sisvetclinic.modules.clinical.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.request.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.VaccineProductOption;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.InventoryDeductionPort.VaccineSupplyUsage;

@Service
public class TreatmentService implements TreatmentUseCase {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final BigDecimal DEFAULT_PROCEDURE_FEE = new BigDecimal("40.00");
    private static final BigDecimal DEFAULT_VACCINATION_FEE = new BigDecimal("35.00");

    private final TreatmentRepositoryPort treatments;
    private final ClinicalRecordPort records;
    private final PatientRepositoryPort patients;
    private final AttentionRepositoryPort attentions;
    private final FollowUpUseCase followUps;
    private final ActorPort actors;
    private final MedicationLookupPort medications;
    private final ClinicalChargePort charges;
    private final InventoryDeductionPort inventory;

    public TreatmentService(
        TreatmentRepositoryPort treatments,
        ClinicalRecordPort records,
        PatientRepositoryPort patients,
        AttentionRepositoryPort attentions,
        FollowUpUseCase followUps,
        ActorPort actors,
        MedicationLookupPort medications
    ) {
        this(treatments, records, patients, attentions, followUps, actors, medications, null, null);
    }

    public TreatmentService(
        TreatmentRepositoryPort treatments,
        ClinicalRecordPort records,
        PatientRepositoryPort patients,
        AttentionRepositoryPort attentions,
        FollowUpUseCase followUps,
        ActorPort actors,
        MedicationLookupPort medications,
        ClinicalChargePort charges
    ) {
        this(treatments, records, patients, attentions, followUps, actors, medications, charges, null);
    }

    @Autowired
    public TreatmentService(
        TreatmentRepositoryPort treatments,
        ClinicalRecordPort records,
        PatientRepositoryPort patients,
        AttentionRepositoryPort attentions,
        FollowUpUseCase followUps,
        ActorPort actors,
        MedicationLookupPort medications,
        ClinicalChargePort charges,
        InventoryDeductionPort inventory
    ) {
        this.treatments = treatments;
        this.records = records;
        this.patients = patients;
        this.attentions = attentions;
        this.followUps = followUps;
        this.actors = actors;
        this.medications = medications;
        this.charges = charges;
        this.inventory = inventory;
    }

    @Transactional
    public Treatment registerTreatment(TreatmentRequest r) {
        Actor actor = vet();
        MedicalConsultation consultation = activeConsultation(r.consultationId());

        if (r.description() == null || r.description().isBlank() ||
            r.instructions() == null || r.instructions().isBlank()) {
            throw new IllegalArgumentException(
                "Tratamiento e indicaciones requeridos"
            );
        }

        return treatments.saveTreatment(
            new Treatment(
                0, consultation.id(), r.description(), r.instructions(),
                r.productId(), LocalDateTime.now(LIMA_ZONE), actor.id()
            )
        );
    }

    @Transactional
    public ClinicalProcedure registerProcedure(ProcedureRequest r) {
        Actor actor = vet();
        MedicalConsultation consultation = activeConsultation(r.consultationId());

        if (r.name() == null || r.name().isBlank()) {
            throw new IllegalArgumentException(
                "Indique procedimiento realizado"
            );
        }

        ClinicalProcedure saved = treatments.saveProcedure(
            new ClinicalProcedure(
                0, consultation.id(), r.name(), r.observations(),
                LocalDateTime.now(LIMA_ZONE), actor.id()
            )
        );

        if (charges != null) {
            charges.registerCharge(
                consultation.id(),
                "Procedimiento clínico: " + r.name().trim(),
                DEFAULT_PROCEDURE_FEE
            );
        }

        return saved;
    }

    @Transactional
    public Vaccination registerVaccination(VaccinationRequest r) {
        Actor actor = vet();

        if (r == null || patients.findById(r.petId()) == null ||
            r.dose() == null || r.dose().isBlank()) {
            throw new IllegalArgumentException(
                "Paciente y dosis son requeridos"
            );
        }

        if ((r.productId() == null) && (r.vaccine() == null || r.vaccine().isBlank())) {
            throw new IllegalArgumentException(
                "Indique el nombre de la vacuna o seleccione un producto de inventario"
            );
        }

        if (r.applicationFee() != null && r.applicationFee().signum() < 0) {
            throw new IllegalArgumentException("El precio por aplicación no puede ser negativo");
        }

        ClinicalAttention attention = attentions.findOpenByPet(r.petId());

        if (attention == null || !"ATENDIENDOSE".equals(attention.getStatus())) {
            throw new IllegalStateException(
                "Debe iniciar una atención de vacunación o consulta médica"
            );
        }

        if ("VACCINATION".equals(attention.getType())) {
            if (r.consultationId() != null) {
                throw new IllegalArgumentException(
                    "Una vacunación independiente no requiere consulta médica"
                );
            }
        } else if (attention.requiresMedicalConsultation()) {
            if (r.consultationId() == null) {
                throw new IllegalArgumentException(
                    "Indique la consulta para vacunar durante una atención médica"
                );
            }

            MedicalConsultation consultation = activeConsultation(r.consultationId());

            if (consultation.petId() != r.petId() ||
                consultation.attentionId() != attention.getId()) {
                throw new IllegalArgumentException(
                    "La consulta no pertenece a esta atención y mascota"
                );
            }
        } else {
            throw new IllegalStateException(
                "Este tipo de atención no permite registrar vacunaciones"
            );
        }

        VaccineSupplyUsage supplyUsage = null;
        String vaccineName = r.vaccine() == null ? "" : r.vaccine().trim();
        Integer qtyUsed = null;

        if (r.productId() != null) {
            int qty = r.quantityUsed() == null ? 0 : r.quantityUsed();
            if (qty <= 0) {
                throw new IllegalArgumentException(
                    "Indique la cantidad de ml o unidades a descontar del inventario"
                );
            }
            if (inventory == null) {
                throw new IllegalStateException("El módulo de inventario no está disponible");
            }
            supplyUsage = inventory.deductForVaccination(r.productId(), qty);
            qtyUsed = qty;
            if (vaccineName.isBlank()) {
                vaccineName = supplyUsage.productName();
            }
        }

        Vaccination saved = treatments.saveVaccination(
            new Vaccination(
                0, r.petId(), r.consultationId(), attention.getId(),
                vaccineName, r.dose().trim(), LocalDateTime.now(LIMA_ZONE),
                r.nextDoseDate(), actor.id(),
                r.productId(), qtyUsed
            )
        );

        if (charges != null) {
            if (supplyUsage != null) {
                if (supplyUsage.supplyAmount().signum() > 0) {
                    String supplyDesc = "Insumo vacuna: " + supplyUsage.productName()
                        + " (" + supplyUsage.quantity() + " " + supplyUsage.baseUnit() + ")";
                    if (r.consultationId() != null) {
                        charges.registerCharge(r.consultationId(), supplyDesc, supplyUsage.supplyAmount());
                    } else {
                        charges.registerAttentionCharge(attention.getId(), supplyDesc, supplyUsage.supplyAmount());
                    }
                }
                BigDecimal extraFee = r.applicationFee() != null ? r.applicationFee() : BigDecimal.ZERO;
                if (extraFee.signum() > 0) {
                    String feeDesc = "Servicio aplicación vacuna: " + vaccineName;
                    if (r.consultationId() != null) {
                        charges.registerCharge(r.consultationId(), feeDesc, extraFee);
                    } else {
                        charges.registerAttentionCharge(attention.getId(), feeDesc, extraFee);
                    }
                }
            } else {
                // Vacuna externa o vacía: se cobra el servicio de aplicación
                BigDecimal fee = r.applicationFee() != null ? r.applicationFee() : DEFAULT_VACCINATION_FEE;
                if (fee.signum() > 0) {
                    String feeDesc = "Aplicación de vacuna (jeringa/servicio): " + vaccineName + " (" + r.dose().trim() + ")";
                    if (r.consultationId() != null) {
                        charges.registerCharge(r.consultationId(), feeDesc, fee);
                    } else {
                        charges.registerAttentionCharge(attention.getId(), feeDesc, fee);
                    }
                }
            }
        }

        if ("VACCINATION".equals(attention.getType())) {
            attention.markVaccinationCompleted();
            attentions.save(attention);
        }

        if (r.nextDoseDate() != null) {
            followUps.scheduleFollowUp(
                new FollowUpRequest(
                    r.petId(), r.consultationId(),
                    "Próxima dosis de " + vaccineName,
                    r.nextDoseDate().atTime(9, 0), null
                )
            );
        }

        return saved;
    }

    @Transactional
    public Referral registerReferral(ReferralRequest r) {
        Actor actor = vet();
        MedicalConsultation consultation = activeConsultation(r.consultationId());

        if (r.reason() == null || r.reason().isBlank()) {
            throw new IllegalArgumentException("Indique motivo de derivación");
        }

        return treatments.saveReferral(
            new Referral(
                0, consultation.id(), r.reason(), r.destination(),
                LocalDateTime.now(LIMA_ZONE), actor.id()
            )
        );
    }

    public List<MedicationDetail> getMedicationOptions(String search) {
        vet();
        return medications.search(search == null ? "" : search);
    }

    public List<VaccineProductOption> getVaccineOptions(String search) {
        ClinicalAccess.require(
            actors, "ADMINISTRADOR", "ADMIN", "RECEPCIONISTA",
            "RECEPTIONIST", "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
        return medications.searchVaccines(search == null ? "" : search);
    }

    private Actor vet() {
        return ClinicalAccess.require(
            actors, "VETERINARIO", "VETERINARIAN", "ESPECIALISTA"
        );
    }

    private MedicalConsultation activeConsultation(long id) {
        MedicalConsultation consultation = records.findConsultation(id);

        if (consultation == null || "COMPLETED".equals(consultation.status())) {
            throw new IllegalStateException("Consulta no disponible");
        }

        return consultation;
    }
}
