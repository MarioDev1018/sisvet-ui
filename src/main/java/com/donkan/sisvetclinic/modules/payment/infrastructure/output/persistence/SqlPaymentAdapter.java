package com.donkan.sisvetclinic.modules.payment.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.payment.domain.PaymentOrder;
import com.donkan.sisvetclinic.modules.payment.domain.state.PaymentStateFactory;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlPaymentAdapter implements PaymentRepositoryPort {

    private final JdbcTemplate jdbc;

    private static final String SELECT = """
        SELECT id, operation_reference, provider, original_amount, original_currency,
               charged_amount, charged_currency, provider_order_id, capture_reference,
               approval_url, status, created_at, updated_at
        FROM [dbo].[PaymentOrder]
        """;

    public SqlPaymentAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(PaymentOrder order) {

        String sql = """
            INSERT INTO [dbo].[PaymentOrder]
                (id, operation_reference, provider, original_amount, original_currency,
                 charged_amount, charged_currency, provider_order_id, capture_reference,
                 approval_url, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try {

            return jdbc.update(
                    sql,
                    order.id(),
                    order.operationReference(),
                    order.provider(),
                    order.originalAmount(),
                    order.originalCurrency(),
                    order.chargedAmount(),
                    order.chargedCurrency(),
                    order.providerOrderId(),
                    order.captureReference(),
                    order.approvalUrl(),
                    order.statusCode(),
                    Timestamp.valueOf(order.createdAt()),
                    Timestamp.valueOf(order.updatedAt())
            ) == 1;

        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void update(PaymentOrder order, String expectedStatus) {

        String sql = """
            UPDATE [dbo].[PaymentOrder]
            SET charged_amount = ?,
                charged_currency = ?,
                provider_order_id = ?,
                capture_reference = ?,
                approval_url = ?,
                status = ?,
                updated_at = ?
            WHERE id = ?
              AND status = ?
            """;

        int updated = jdbc.update(
                sql,
                order.chargedAmount(),
                order.chargedCurrency(),
                order.providerOrderId(),
                order.captureReference(),
                order.approvalUrl(),
                order.statusCode(),
                Timestamp.valueOf(order.updatedAt()),
                order.id(),
                expectedStatus
        );

        if (updated != 1) {

            throw new IllegalStateException(
                    "El pago cambió de estado durante la actualización."
            );
        }
    }

    @Override
    public boolean tryStartCreation(String paymentId) {

        String sql = """
            UPDATE [dbo].[PaymentOrder]
            SET status = 'CREATING',
                updated_at = SYSUTCDATETIME()
            WHERE id = ?
              AND provider_order_id IS NULL
              AND (
                  status IN ('PENDING', 'UNKNOWN')
                  OR (
                      status = 'CREATING'
                      AND updated_at < DATEADD(MINUTE, -2, SYSUTCDATETIME())
                  )
              )
            """;

        return jdbc.update(sql, paymentId) == 1;
    }

    @Override
    public boolean tryStartVerification(String paymentId) {

        String sql = """
            UPDATE [dbo].[PaymentOrder]
            SET status = 'VERIFYING',
                updated_at = SYSUTCDATETIME()
            WHERE id = ?
              AND provider_order_id IS NOT NULL
              AND (
                  status IN ('AWAITING_APPROVAL', 'UNKNOWN')
                  OR (
                      status = 'VERIFYING'
                      AND updated_at < DATEADD(MINUTE, -2, SYSUTCDATETIME())
                  )
              )
            """;

        return jdbc.update(sql, paymentId) == 1;
    }

    @Override
    public Optional<PaymentOrder> findById(String id) {

        return jdbc.query(
                SELECT + " WHERE id = ?",
                (rs, row) -> map(rs),
                id
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByOperationReference(String reference) {

        return jdbc.query(
                SELECT + " WHERE operation_reference = ?",
                (rs, row) -> map(rs),
                reference
        ).stream().findFirst();
    }

    @Override
    public Optional<PaymentOrder> findByProviderOrderId(String providerOrderId) {

        return jdbc.query(
                SELECT + " WHERE provider_order_id = ?",
                (rs, row) -> map(rs),
                providerOrderId
        ).stream().findFirst();
    }

    @Override
    public String simulationDecision(String providerOrderId) {

        List<String> values = jdbc.query(
                """
                SELECT simulation_decision
                FROM [dbo].[PaymentOrder]
                WHERE provider = 'PAYPAL'
                  AND provider_order_id = ?
                """,
                (rs, row) -> rs.getString(1),
                providerOrderId
        );

        return values.isEmpty() ? null : values.getFirst();
    }

    @Override
    public void decideSimulation(String providerOrderId, String decision) {

        if (!"APPROVED".equals(decision)
                && !"REJECTED".equals(decision)) {

            throw new IllegalArgumentException(
                    "Decisión de simulación inválida."
            );
        }

        String sql = """
            UPDATE [dbo].[PaymentOrder]
            SET simulation_decision = ?,
                updated_at = SYSUTCDATETIME()
            WHERE provider = 'PAYPAL'
              AND provider_order_id = ?
              AND status = 'AWAITING_APPROVAL'
              AND simulation_decision = 'PENDING'
            """;

        if (jdbc.update(sql, decision, providerOrderId) != 1
                && !decision.equals(simulationDecision(providerOrderId))) {

            throw new IllegalStateException(
                    "La orden no existe, ya fue procesada o cambió de decisión."
            );
        }
    }

    private PaymentOrder map(ResultSet rs) throws SQLException {

        return new PaymentOrder(
                rs.getString("id"),
                rs.getString("operation_reference"),
                rs.getString("provider"),
                rs.getBigDecimal("original_amount"),
                rs.getString("original_currency"),
                rs.getBigDecimal("charged_amount"),
                rs.getString("charged_currency"),
                rs.getString("provider_order_id"),
                rs.getString("capture_reference"),
                rs.getString("approval_url"),
                PaymentStateFactory.fromCode(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}