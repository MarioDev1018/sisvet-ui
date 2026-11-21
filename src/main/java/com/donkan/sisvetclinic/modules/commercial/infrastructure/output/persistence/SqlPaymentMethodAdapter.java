package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentMethodRepositoryPort;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlPaymentMethodAdapter implements PaymentMethodRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlPaymentMethodAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PaymentMethod> findById(Long paymentMethodId) {

        String sql = "SELECT id, name, is_active FROM [dbo].[PaymentMethod] WHERE id = ?";

        List<PaymentMethod> result = jdbc.query(sql, (rs, row) ->
                new PaymentMethod(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBoolean("is_active")
                ), paymentMethodId);

        return result.stream().findFirst();
    }

    @Override
    public List<PaymentMethod> findAllActive() {

        String sql = """
            SELECT id, name, is_active
            FROM [dbo].[PaymentMethod]
            WHERE is_active = 1
            ORDER BY name
            """;

        return jdbc.query(sql, (rs, row) ->
                new PaymentMethod(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBoolean("is_active")
                ));
    }
}