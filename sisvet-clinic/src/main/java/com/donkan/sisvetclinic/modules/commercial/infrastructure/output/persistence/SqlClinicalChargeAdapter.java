package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalCharge;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.ClinicalChargePort;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlClinicalChargeAdapter implements ClinicalChargePort {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public SqlClinicalChargeAdapter(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public List<ClinicalCharge> findPendingByConsultation(Long consultationId) {

        String sql = """
            SELECT id, consultation_id, description, amount, is_charged
            FROM [dbo].[ClinicalCharge]
            WHERE consultation_id = ?
              AND is_charged = 0
              AND sale_id IS NULL
            ORDER BY id
            """;

        return jdbc.query(sql, (rs, row) -> new ClinicalCharge(
                rs.getLong("id"),
                rs.getLong("consultation_id"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getBoolean("is_charged")
        ), consultationId);
    }

    @Override
    public void markAsCharged(List<Long> chargeIds, Long saleId) {

        if (chargeIds == null || chargeIds.isEmpty()) {
            throw new IllegalArgumentException("No se seleccionaron conceptos clínicos.");
        }

        String sql = """
            UPDATE [dbo].[ClinicalCharge]
            SET is_charged = 1,
                sale_id = :saleId
            WHERE id IN (:chargeIds)
              AND is_charged = 0
              AND sale_id IS NULL
            """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("saleId", saleId)
                .addValue("chargeIds", chargeIds);

        int updated = namedJdbc.update(sql, parameters);

        if (updated != chargeIds.size()) {
            throw new IllegalStateException(
                    "Uno o más conceptos clínicos ya fueron cobrados o no están disponibles."
            );
        }
    }
}