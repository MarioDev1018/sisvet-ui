package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Supplier;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SupplierRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlSupplierAdapter implements SupplierRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlSupplierAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_SUPPLIERS = """
            SELECT id, ruc, business_name, phone, email, record_status
            FROM [dbo].[Supplier]
            """;

    @Override
    public Supplier save(Supplier supplier) {

        String sql = """
            INSERT INTO [dbo].[Supplier]
                (ruc, business_name, phone, email, record_status)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                supplier.getRuc(),
                supplier.getBusinessName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.isActive() ? "Active" : "Inactive");

        return findById(id).orElseThrow();
    }

    @Override
    public Optional<Supplier> findById(Long supplierId) {

        List<Supplier> result = jdbc.query(
                SELECT_SUPPLIERS + " WHERE id = ?",
                (rs, row) -> mapSupplier(rs), supplierId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Supplier> findByRuc(String ruc) {

        List<Supplier> result = jdbc.query(
                SELECT_SUPPLIERS + " WHERE ruc = ?",
                (rs, row) -> mapSupplier(rs), ruc);

        return result.stream().findFirst();
    }

    @Override
    public List<Supplier> findAll() {

        return jdbc.query(
                SELECT_SUPPLIERS + " ORDER BY business_name",
                (rs, row) -> mapSupplier(rs));
    }

    @Override
    public boolean existsByRuc(String ruc) {

        String sql = "SELECT COUNT(*) FROM [dbo].[Supplier] WHERE ruc = ?";

        Integer count = jdbc.queryForObject(sql, Integer.class, ruc);

        return count != null && count > 0;
    }

    private Supplier mapSupplier(ResultSet rs) throws SQLException {

        return new Supplier(
                rs.getLong("id"),
                rs.getString("ruc"),
                rs.getString("business_name"),
                rs.getString("phone"),
                rs.getString("email"),
                "Active".equalsIgnoreCase(rs.getString("record_status"))
        );
    }
}