package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.donkan.sisvetclinic.modules.clinical.domain.*;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.*;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.*;

@Repository
public class SqlClinicalQueryAdapter implements ClinicalQueryPort {

    private final JdbcTemplate jdbc;
    private final ClinicalRecordPort records;
    private final TreatmentRepositoryPort treatments;

    public SqlClinicalQueryAdapter(
        JdbcTemplate jdbc,
        ClinicalRecordPort records,
        TreatmentRepositoryPort treatments
    ) {
        this.jdbc = jdbc;
        this.records = records;
        this.treatments = treatments;
    }

    private static final RowMapper<PetSummary> PET_SUMMARY_MAPPER = (r, n) -> {
        String rawSpecies = r.getString("species");
        return new PetSummary(
            r.getLong("id"),
            r.getLong("owner_id"),
            r.getString("owner_name"),
            r.getString("owner_document"),
            r.getString("owner_phone"),
            r.getString("name"),
            ClinicalSpecies.canonicalCode(rawSpecies),
            ClinicalSpecies.displayLabel(rawSpecies),
            r.getString("breed"),
            r.getString("sex"),
            r.getDate("birth_date") == null ? null : r.getDate("birth_date").toLocalDate(),
            (Integer) r.getObject("estimated_age_months"),
            r.getBigDecimal("last_weight_kg"),
            SqlClinicalMappers.dateTime(r, "last_weight_at"),
            SqlClinicalMappers.nullableLong(r, "active_attention_id"),
            r.getString("active_attention_state"),
            r.getString("active_attention_type")
        );
    };

    private static final String PET_SUMMARY_SELECT = """
        SELECT p.id, p.owner_id, c.full_name AS owner_name, c.document_number AS owner_document, c.phone AS owner_phone,
               p.name, p.species, p.breed, p.sex, p.birth_date, p.estimated_age_months,
               lw.weight AS last_weight_kg, lw.created_at AS last_weight_at,
               att.id AS active_attention_id, att.state AS active_attention_state, att.type AS active_attention_type
        FROM dbo.ClinicalPet p
        JOIN dbo.Customer c ON c.id = p.owner_id
        OUTER APPLY (
            SELECT TOP 1 mc.weight, mc.created_at
            FROM dbo.MedicalConsultation mc
            WHERE mc.pet_id = p.id
            ORDER BY mc.created_at DESC, mc.id DESC
        ) lw
        OUTER APPLY (
            SELECT TOP 1 ca.id, ca.state, ca.type
            FROM dbo.ClinicalAttention ca
            WHERE ca.pet_id = p.id AND ca.state <> 'ATENDIDO'
            ORDER BY ca.id DESC
        ) att
        """;

    @Override
    public List<PetSummary> searchPetSummaries(String criteria) {
        String term = "%" + (criteria == null ? "" : criteria) + "%";
        return jdbc.query(
            PET_SUMMARY_SELECT +
            "WHERE p.name LIKE ? OR c.full_name LIKE ? OR c.document_number LIKE ? OR CAST(p.id AS VARCHAR(30)) LIKE ? " +
            "ORDER BY p.name",
            PET_SUMMARY_MAPPER,
            term, term, term, term
        );
    }

    @Override
    public PetSummary findPetSummary(long petId) {
        return jdbc.query(
            PET_SUMMARY_SELECT + "WHERE p.id = ?",
            PET_SUMMARY_MAPPER,
            petId
        ).stream().findFirst().orElse(null);
    }

    @Override
    public List<AttentionSummary> findActiveAttentions() {
        String sql = """
            SELECT ca.id, ca.pet_id, p.name AS pet_name, p.species,
                   p.owner_id, c.full_name AS owner_name, c.document_number AS owner_document,
                   ca.type, ca.priority, ca.urgency_description, ca.priority_assigned_by,
                   ca.state, ca.arrived_at, ca.requires_grooming, ca.clinical_closed,
                   ca.consultation_id, ca.vaccination_completed, ca.[version],
                   lw.weight AS last_weight_kg
            FROM dbo.ClinicalAttention ca
            JOIN dbo.ClinicalPet p ON p.id = ca.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            OUTER APPLY (
                SELECT TOP 1 mc.weight
                FROM dbo.MedicalConsultation mc
                WHERE mc.pet_id = p.id
                ORDER BY mc.created_at DESC, mc.id DESC
            ) lw
            WHERE ca.state <> 'ATENDIDO' AND ca.arrived_at IS NOT NULL
            ORDER BY
                CASE ca.state
                    WHEN 'EN_ESPERA' THEN 1
                    WHEN 'ATENDIENDOSE' THEN 2
                    WHEN 'EN_BANO' THEN 3
                    WHEN 'EN_PELUQUERIA' THEN 4
                    WHEN 'LISTO_RECOJO' THEN 5
                    ELSE 6
                END,
                CASE WHEN ca.priority IS NULL THEN 1 ELSE 0 END,
                ca.priority ASC,
                ca.arrived_at ASC
            """;

        return jdbc.query(sql, (r, n) -> {
            String rawSpecies = r.getString("species");
            return new AttentionSummary(
                r.getLong("id"),
                r.getLong("pet_id"),
                r.getString("pet_name"),
                ClinicalSpecies.canonicalCode(rawSpecies),
                ClinicalSpecies.displayLabel(rawSpecies),
                r.getLong("owner_id"),
                r.getString("owner_name"),
                r.getString("owner_document"),
                r.getString("type"),
                (Integer) r.getObject("priority"),
                r.getString("urgency_description"),
                SqlClinicalMappers.nullableLong(r, "priority_assigned_by"),
                r.getString("state"),
                SqlClinicalMappers.dateTime(r, "arrived_at"),
                r.getBoolean("requires_grooming"),
                r.getBoolean("clinical_closed"),
                SqlClinicalMappers.nullableLong(r, "consultation_id"),
                r.getBoolean("vaccination_completed"),
                r.getBigDecimal("last_weight_kg"),
                r.getLong("version")
            );
        });
    }

    @Override
    public List<ProfessionalOption> listActiveProfessionals() {
        List<ProfessionalOption> base = jdbc.query(
            "SELECT u.id, u.full_name, r.name AS role_name " +
            "FROM dbo.[User] u " +
            "JOIN dbo.Role r ON r.id = u.role_id " +
            "WHERE u.record_status = 'Active' " +
            "AND UPPER(r.name) IN ('VETERINARIO','VETERINARIAN','ESPECIALISTA','SPECIALIST') " +
            "ORDER BY u.full_name",
            (r, n) -> new ProfessionalOption(
                r.getLong("id"),
                r.getString("full_name"),
                r.getString("role_name"),
                List.of()
            )
        );

        Map<Long, List<String>> specialtiesMap = new HashMap<>();
        jdbc.query(
            "SELECT professional_id, specialty FROM dbo.ClinicalProfessionalSpecialty ORDER BY specialty",
            r -> {
                long profId = r.getLong("professional_id");
                specialtiesMap.computeIfAbsent(profId, k -> new ArrayList<>())
                        .add(r.getString("specialty"));
            }
        );

        return base.stream()
                .map(p -> new ProfessionalOption(
                    p.id(),
                    p.fullName(),
                    p.roleName(),
                    specialtiesMap.getOrDefault(p.id(), List.of())
                ))
                .toList();
    }

    @Override
    public List<AppointmentView> findAppointmentViews(Long professionalId, LocalDateTime start, LocalDateTime end) {
        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.pet_id, p.name AS pet_name, p.species,
                   p.owner_id, c.full_name AS owner_name,
                   a.professional_id, u.full_name AS professional_name,
                   a.reason, a.starts_at, a.ends_at, a.status, a.specialty,
                   a.consultation_id, a.created_at, a.reminder_sent
            FROM dbo.ClinicalAppointment a
            JOIN dbo.ClinicalPet p ON p.id = a.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            JOIN dbo.[User] u ON u.id = a.professional_id
            WHERE a.status = 'CONFIRMED'
              AND a.starts_at < ? AND a.ends_at > ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(end);
        params.add(start);
        if (professionalId != null && professionalId > 0) {
            sql.append(" AND a.professional_id = ?");
            params.add(professionalId);
        }
        sql.append(" ORDER BY a.starts_at ASC");

        return jdbc.query(sql.toString(), APPOINTMENT_VIEW_MAPPER, params.toArray());
    }

    @Override
    public List<AppointmentView> listAppointmentsByPet(long petId) {
        String sql = """
            SELECT a.id, a.pet_id, p.name AS pet_name, p.species,
                   p.owner_id, c.full_name AS owner_name,
                   a.professional_id, u.full_name AS professional_name,
                   a.reason, a.starts_at, a.ends_at, a.status, a.specialty,
                   a.consultation_id, a.created_at, a.reminder_sent
            FROM dbo.ClinicalAppointment a
            JOIN dbo.ClinicalPet p ON p.id = a.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            JOIN dbo.[User] u ON u.id = a.professional_id
            WHERE a.pet_id = ?
            ORDER BY a.starts_at DESC
            """;
        return jdbc.query(sql, APPOINTMENT_VIEW_MAPPER, petId);
    }

    @Override
    public List<FollowUpView> findOutstandingFollowUpViews() {
        String sql = """
            SELECT f.id, f.pet_id, p.name AS pet_name, p.species,
                   p.owner_id, c.full_name AS owner_name, c.phone AS owner_phone,
                   f.consultation_id, f.reason, f.due_at, f.status, f.reviewed_at, f.appointment_id
            FROM dbo.ClinicalFollowUp f
            JOIN dbo.ClinicalPet p ON p.id = f.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            WHERE f.status IN ('PENDING', 'REVIEWED')
            ORDER BY f.due_at ASC
            """;
        return jdbc.query(sql, FOLLOWUP_VIEW_MAPPER);
    }

    @Override
    public List<FollowUpView> listFollowUpViewsByPet(long petId) {
        String sql = """
            SELECT f.id, f.pet_id, p.name AS pet_name, p.species,
                   p.owner_id, c.full_name AS owner_name, c.phone AS owner_phone,
                   f.consultation_id, f.reason, f.due_at, f.status, f.reviewed_at, f.appointment_id
            FROM dbo.ClinicalFollowUp f
            JOIN dbo.ClinicalPet p ON p.id = f.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            WHERE f.pet_id = ?
            ORDER BY f.due_at DESC
            """;
        return jdbc.query(sql, FOLLOWUP_VIEW_MAPPER, petId);
    }

    @Override
    public ConsultationDetailView findConsultationDetail(long consultationId) {
        String sql = """
            SELECT mc.*, p.name AS pet_name, p.species, p.owner_id,
                   c.full_name AS owner_name, u.full_name AS veterinarian_name
            FROM dbo.MedicalConsultation mc
            JOIN dbo.ClinicalPet p ON p.id = mc.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            JOIN dbo.[User] u ON u.id = mc.veterinarian_id
            WHERE mc.id = ?
            """;

        return jdbc.query(sql, (r, n) -> mapConsultationDetail(r), consultationId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public List<ConsultationDetailView> findOpenConsultationDetails() {
        String sql = """
            SELECT mc.*, p.name AS pet_name, p.species, p.owner_id,
                   c.full_name AS owner_name, u.full_name AS veterinarian_name
            FROM dbo.MedicalConsultation mc
            JOIN dbo.ClinicalPet p ON p.id = mc.pet_id
            JOIN dbo.Customer c ON c.id = p.owner_id
            JOIN dbo.[User] u ON u.id = mc.veterinarian_id
            WHERE mc.status = 'OPEN'
            ORDER BY mc.created_at DESC
            """;

        return jdbc.query(sql, (r, n) -> mapConsultationDetail(r));
    }

    @Override
    public MedicalHistoryView buildMedicalHistoryView(long petId) {
        PetSummary pet = findPetSummary(petId);
        if (pet == null) {
            return null;
        }
        List<MedicalConsultation> consultations = records.listByPet(petId);
        List<MedicalHistoryView.WeightPoint> weights = consultations.stream()
                .map(c -> new MedicalHistoryView.WeightPoint(c.id(), c.createdAt(), c.weight(), c.reason()))
                .toList();

        return new MedicalHistoryView(
            pet,
            weights,
            consultations,
            records.listDiagnosesByPet(petId),
            treatments.listTreatmentsByPet(petId),
            treatments.listProceduresByPet(petId),
            treatments.listVaccinations(petId),
            treatments.listReferralsByPet(petId),
            listAppointmentsByPet(petId),
            listFollowUpViewsByPet(petId)
        );
    }

    private ConsultationDetailView mapConsultationDetail(java.sql.ResultSet r) throws java.sql.SQLException {
        long consultationId = r.getLong("id");
        String rawSpecies = r.getString("species");

        List<Diagnosis> diagnoses = jdbc.query(
            "SELECT * FROM dbo.ClinicalDiagnosis WHERE consultation_id = ? ORDER BY created_at DESC",
            SqlClinicalMappers.DIAGNOSIS, consultationId
        );
        List<Treatment> txList = jdbc.query(
            "SELECT * FROM dbo.ClinicalTreatment WHERE consultation_id = ? ORDER BY prescribed_at DESC",
            SqlClinicalMappers.TREATMENT, consultationId
        );
        List<ClinicalProcedure> procList = jdbc.query(
            "SELECT * FROM dbo.ClinicalProcedure WHERE consultation_id = ? ORDER BY performed_at DESC",
            SqlClinicalMappers.PROCEDURE, consultationId
        );
        List<Vaccination> vaxList = jdbc.query(
            "SELECT * FROM dbo.ClinicalVaccination WHERE consultation_id = ? ORDER BY applied_at DESC",
            SqlClinicalMappers.VACCINATION, consultationId
        );
        List<Referral> refList = jdbc.query(
            "SELECT * FROM dbo.ClinicalReferral WHERE consultation_id = ? ORDER BY created_at DESC",
            SqlClinicalMappers.REFERRAL, consultationId
        );

        return new ConsultationDetailView(
            consultationId,
            r.getLong("attention_id"),
            r.getLong("pet_id"),
            r.getString("pet_name"),
            ClinicalSpecies.canonicalCode(rawSpecies),
            ClinicalSpecies.displayLabel(rawSpecies),
            r.getLong("owner_id"),
            r.getString("owner_name"),
            r.getLong("veterinarian_id"),
            r.getString("veterinarian_name"),
            r.getString("reason"),
            r.getBigDecimal("weight"),
            r.getString("symptoms"),
            r.getString("observations"),
            r.getString("status"),
            SqlClinicalMappers.dateTime(r, "created_at"),
            diagnoses,
            txList,
            procList,
            vaxList,
            refList
        );
    }

    private static final RowMapper<AppointmentView> APPOINTMENT_VIEW_MAPPER = (r, n) -> {
        String rawSpecies = r.getString("species");
        return new AppointmentView(
            r.getLong("id"),
            r.getLong("pet_id"),
            r.getString("pet_name"),
            ClinicalSpecies.displayLabel(rawSpecies),
            r.getLong("owner_id"),
            r.getString("owner_name"),
            r.getLong("professional_id"),
            r.getString("professional_name"),
            r.getString("reason"),
            SqlClinicalMappers.dateTime(r, "starts_at"),
            SqlClinicalMappers.dateTime(r, "ends_at"),
            r.getString("status"),
            r.getString("specialty"),
            SqlClinicalMappers.nullableLong(r, "consultation_id"),
            SqlClinicalMappers.dateTime(r, "created_at"),
            r.getBoolean("reminder_sent")
        );
    };

    private static final RowMapper<FollowUpView> FOLLOWUP_VIEW_MAPPER = (r, n) -> {
        String rawSpecies = r.getString("species");
        return new FollowUpView(
            r.getLong("id"),
            r.getLong("pet_id"),
            r.getString("pet_name"),
            ClinicalSpecies.displayLabel(rawSpecies),
            r.getLong("owner_id"),
            r.getString("owner_name"),
            r.getString("owner_phone"),
            SqlClinicalMappers.nullableLong(r, "consultation_id"),
            r.getString("reason"),
            SqlClinicalMappers.dateTime(r, "due_at"),
            r.getString("status"),
            SqlClinicalMappers.dateTime(r, "reviewed_at"),
            SqlClinicalMappers.nullableLong(r, "appointment_id")
        );
    };
}
