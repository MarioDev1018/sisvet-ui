package com.donkan.sisvetclinic.modules.notification.infrastructure.output.persistence;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.donkan.sisvetclinic.modules.notification.service.dto.response.NotificationResult;
import com.donkan.sisvetclinic.modules.notification.service.port.out.RecipientDirectoryPort;

@Repository
public class SqlRecipientDirectoryAdapter implements RecipientDirectoryPort {

    private final JdbcTemplate jdbc;

    public SqlRecipientDirectoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RecipientContact findCustomerContact(long customerId) {
        List<RecipientContact> list = jdbc.query(
            "SELECT id, full_name, email FROM dbo.Customer WHERE id = ? AND record_status = 'Active'",
            (rs, row) -> new RecipientContact(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email")
            ),
            customerId
        );
        return list.stream().findFirst().orElse(null);
    }

    @Override
    public NotificationResult findByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        List<NotificationResult> list = jdbc.query(
            "SELECT id, sent_ok, detail FROM dbo.NotificationLog WHERE idempotency_key = ?",
            (rs, row) -> new NotificationResult(
                rs.getLong("id"),
                rs.getBoolean("sent_ok"),
                rs.getString("detail")
            ),
            key
        );
        return list.stream().findFirst().orElse(null);
    }

    @Override
    public long saveLog(long recipientId, String email, String channel, String key,
                        String subject, String body, boolean sentOk, String detail) {
        Long id = jdbc.queryForObject(
            "INSERT INTO dbo.NotificationLog " +
            "(recipient_id, recipient_email, channel, idempotency_key, subject, body, sent_ok, detail) " +
            "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            Long.class,
            recipientId,
            email,
            channel,
            (key == null || key.isBlank()) ? null : key,
            subject,
            body != null && body.length() > 2000 ? body.substring(0, 2000) : body,
            sentOk,
            detail
        );
        return id == null ? 0 : id;
    }
}
