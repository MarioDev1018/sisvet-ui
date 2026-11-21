package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
import com.donkan.sisvetclinic.modules.clinical.domain.*;

final class SqlClinicalMappers {

    private SqlClinicalMappers() {}

    static Long nullableLong(ResultSet result, String column)
        throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    static LocalDateTime dateTime(ResultSet result, String column)
        throws SQLException {
        var timestamp = result.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static final RowMapper<Pet> PET = (r, n) -> new Pet(
        r.getLong("id"), r.getLong("owner_id"), r.getString("name"),
        r.getString("species"), r.getString("breed"), r.getString("sex"),
        r.getDate("birth_date") == null
            ? null : r.getDate("birth_date").toLocalDate(),
        (Integer) r.getObject("estimated_age_months")
    );

    static final RowMapper<ClinicalAttention> ATTENTION = (r, n) ->
        new ClinicalAttention(
            r.getLong("id"), r.getLong("pet_id"), r.getString("type"),
            (Integer) r.getObject("priority"),
            r.getString("urgency_description"),
            nullableLong(r, "priority_assigned_by"), r.getString("state"),
            dateTime(r, "arrived_at"), r.getBoolean("requires_grooming"),
            r.getBoolean("clinical_closed"), nullableLong(r, "consultation_id"),
            r.getBoolean("vaccination_completed"), r.getLong("version")
        );

    static final RowMapper<MedicalConsultation> CONSULTATION = (r, n) ->
        new MedicalConsultation(
            r.getLong("id"), r.getLong("attention_id"),
            r.getLong("pet_id"), r.getLong("veterinarian_id"),
            r.getString("reason"), r.getBigDecimal("weight"),
            r.getString("symptoms"), r.getString("observations"),
            r.getString("status"), dateTime(r, "created_at")
        );

    static final RowMapper<Diagnosis> DIAGNOSIS = (r, n) -> new Diagnosis(
        r.getLong("id"), r.getLong("consultation_id"),
        r.getString("description"), r.getString("indications"),
        r.getLong("author_id"), dateTime(r, "created_at")
    );

    static final RowMapper<Treatment> TREATMENT = (r, n) -> new Treatment(
        r.getLong("id"), r.getLong("consultation_id"),
        r.getString("description"), r.getString("instructions"),
        nullableLong(r, "product_id"), dateTime(r, "prescribed_at"),
        r.getLong("author_id")
    );

    static final RowMapper<ClinicalProcedure> PROCEDURE = (r, n) ->
        new ClinicalProcedure(
            r.getLong("id"), r.getLong("consultation_id"),
            r.getString("name"), r.getString("observations"),
            dateTime(r, "performed_at"), r.getLong("author_id")
        );

    static final RowMapper<Vaccination> VACCINATION = (r, n) ->
        new Vaccination(
            r.getLong("id"), r.getLong("pet_id"),
            nullableLong(r, "consultation_id"), nullableLong(r, "attention_id"),
            r.getString("vaccine"), r.getString("dose"),
            dateTime(r, "applied_at"),
            r.getDate("next_dose_date") == null
                ? null : r.getDate("next_dose_date").toLocalDate(),
            r.getLong("veterinarian_id"),
            nullableLong(r, "product_id"),
            (Integer) r.getObject("quantity_used")
        );

    static final RowMapper<Referral> REFERRAL = (r, n) -> new Referral(
        r.getLong("id"), r.getLong("consultation_id"),
        r.getString("reason"), r.getString("destination"),
        dateTime(r, "created_at"), r.getLong("author_id")
    );

    static final RowMapper<Appointment> APPOINTMENT = (r, n) ->
        new Appointment(
            r.getLong("id"), r.getLong("pet_id"),
            r.getLong("professional_id"), r.getString("reason"),
            dateTime(r, "starts_at"), dateTime(r, "ends_at"),
            r.getString("status"), r.getString("specialty"),
            nullableLong(r, "consultation_id"), dateTime(r, "created_at"),
            r.getBoolean("reminder_sent")
        );

    static final RowMapper<FollowUp> FOLLOWUP = (r, n) -> new FollowUp(
        r.getLong("id"), r.getLong("pet_id"),
        nullableLong(r, "consultation_id"), r.getString("reason"),
        dateTime(r, "due_at"), r.getString("status"),
        dateTime(r, "reviewed_at"), nullableLong(r, "appointment_id")
    );
}
