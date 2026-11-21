package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalPaymentSession;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.ClinicalPaymentSessionPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SqlClinicalPaymentSessionAdapter implements ClinicalPaymentSessionPort {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private final JdbcTemplate jdbc;

    private static final String SELECT = """
        SELECT operation_key, user_id, consultation_id, attention_id,
               customer_id, payment_method_id, provider_code, charge_ids,
               total_amount, request_fingerprint, status, payment_id, sale_id
        FROM [dbo].[ClinicalPaymentSession]
        """;

    public SqlClinicalPaymentSessionAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public ClinicalPaymentSession saveOrGetExisting(ClinicalPaymentSession session) {
        List<ClinicalPaymentSession> existing = jdbc.query(
            SELECT + " WITH (UPDLOCK, HOLDLOCK) WHERE operation_key = ?",
            (rs, row) -> map(rs),
            session.operationKey()
        );

        if (!existing.isEmpty()) {
            ClinicalPaymentSession current = existing.get(0);
            if (!current.fingerprint().equals(session.fingerprint())) {
                throw new IllegalStateException("La clave de operación ya fue utilizada con otros conceptos clínicos.");
            }
            return current;
        }

        String chargeIdsCsv = session.chargeIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Timestamp now = Timestamp.valueOf(LocalDateTime.now(LIMA_ZONE));

        jdbc.update(
            """
            INSERT INTO [dbo].[ClinicalPaymentSession]
                (operation_key, user_id, consultation_id, attention_id,
                 customer_id, payment_method_id, provider_code, charge_ids,
                 total_amount, request_fingerprint, status, payment_id, sale_id,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            session.operationKey(),
            session.userId(),
            session.consultationId(),
            session.attentionId(),
            session.customerId(),
            session.paymentMethodId(),
            session.provider(),
            chargeIdsCsv,
            session.totalAmount(),
            session.fingerprint(),
            session.status(),
            session.paymentId(),
            session.saleId(),
            now,
            now
        );

        return session;
    }

    @Override
    public Optional<ClinicalPaymentSession> findByKey(String operationKey) {
        return jdbc.query(
            SELECT + " WHERE operation_key = ?",
            (rs, row) -> map(rs),
            operationKey
        ).stream().findFirst();
    }

    @Override
    public void linkPayment(String operationKey, String paymentId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(LIMA_ZONE));
        jdbc.update(
            "UPDATE [dbo].[ClinicalPaymentSession] SET payment_id = ?, updated_at = ? WHERE operation_key = ? AND payment_id IS NULL",
            paymentId, now, operationKey
        );
    }

    @Override
    public void complete(String operationKey, Long saleId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(LIMA_ZONE));
        jdbc.update(
            "UPDATE [dbo].[ClinicalPaymentSession] SET status = 'COMPLETED', sale_id = ?, updated_at = ? WHERE operation_key = ?",
            saleId, now, operationKey
        );
    }

    @Override
    public void cancel(String operationKey, String paymentId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(LIMA_ZONE));
        jdbc.update(
            "UPDATE [dbo].[ClinicalPaymentSession] SET status = 'CANCELLED', payment_id = COALESCE(payment_id, ?), updated_at = ? WHERE operation_key = ?",
            paymentId, now, operationKey
        );
    }

    @Override
    public void markRecoveryRequired(String operationKey) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(LIMA_ZONE));
        jdbc.update(
            "UPDATE [dbo].[ClinicalPaymentSession] SET status = 'RECOVERY_REQUIRED', updated_at = ? WHERE operation_key = ?",
            now, operationKey
        );
    }

    private ClinicalPaymentSession map(ResultSet rs) throws SQLException {
        String csv = rs.getString("charge_ids");
        List<Long> chargeIds = (csv == null || csv.isBlank())
                ? List.of()
                : Arrays.stream(csv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();

        return new ClinicalPaymentSession(
                rs.getString("operation_key"),
                rs.getLong("user_id"),
                rs.getObject("consultation_id") == null ? null : rs.getLong("consultation_id"),
                rs.getObject("attention_id") == null ? null : rs.getLong("attention_id"),
                rs.getLong("customer_id"),
                rs.getLong("payment_method_id"),
                rs.getString("provider_code"),
                chargeIds,
                rs.getBigDecimal("total_amount"),
                rs.getString("request_fingerprint"),
                rs.getString("status"),
                rs.getString("payment_id"),
                rs.getObject("sale_id") == null ? null : rs.getLong("sale_id")
        );
    }
}
