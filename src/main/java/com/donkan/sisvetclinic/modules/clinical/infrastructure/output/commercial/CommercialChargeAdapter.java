package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.commercial;

import java.math.BigDecimal;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.ClinicalChargePort;

@Repository
public class CommercialChargeAdapter implements ClinicalChargePort {

    private final JdbcTemplate jdbc;

    public CommercialChargeAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isSettled(long consultationId) {
        Integer pending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalCharge " +
            "WHERE consultation_id=? " +
            "AND (is_charged=0 OR sale_id IS NULL)",
            Integer.class, consultationId
        );

        return pending != null && pending == 0;
    }

    @Override
    public boolean isSettledByAttention(long attentionId) {
        Integer pending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.ClinicalCharge " +
            "WHERE attention_id=? " +
            "AND (is_charged=0 OR sale_id IS NULL)",
            Integer.class, attentionId
        );

        return pending != null && pending == 0;
    }

    @Override
    public void registerCharge(long consultationId, String description, BigDecimal amount) {
        if (consultationId <= 0 || description == null || description.isBlank()
                || amount == null || amount.signum() <= 0) {
            return;
        }

        String normalizedDesc = normalizeDescription(description);

        jdbc.update(
            "INSERT INTO dbo.ClinicalCharge " +
            "(consultation_id, attention_id, description, amount, is_charged, sale_id) " +
            "VALUES (?, NULL, ?, ?, 0, NULL)",
            consultationId, normalizedDesc, amount
        );
    }

    @Override
    public void registerAttentionCharge(long attentionId, String description, BigDecimal amount) {
        if (attentionId <= 0 || description == null || description.isBlank()
                || amount == null || amount.signum() <= 0) {
            return;
        }

        String normalizedDesc = normalizeDescription(description);

        jdbc.update(
            "INSERT INTO dbo.ClinicalCharge " +
            "(consultation_id, attention_id, description, amount, is_charged, sale_id) " +
            "VALUES (NULL, ?, ?, ?, 0, NULL)",
            attentionId, normalizedDesc, amount
        );
    }

    private String normalizeDescription(String description) {
        String trimmed = description.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }
}
