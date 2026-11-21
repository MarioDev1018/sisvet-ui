package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Customer;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CustomerRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlCustomerAdapter implements CustomerRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlCustomerAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_CUSTOMERS = """
            SELECT id, document_number, document_type, full_name,
                   phone, email, address, record_status
            FROM [dbo].[Customer]
            """;

    @Override
    public Customer save(Customer customer) {

        String sql = """
            INSERT INTO [dbo].[Customer]
                (document_number, document_type, full_name, phone, email, address, record_status)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                customer.getDocumentNumber(),
                customer.getDocumentType(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress(),
                customer.isActive() ? "Active" : "Inactive");

        return findById(id).orElseThrow();
    }

    @Override
    public Optional<Customer> findById(Long customerId) {

        List<Customer> result = jdbc.query(
                SELECT_CUSTOMERS + " WHERE id = ?",
                (rs, row) -> mapCustomer(rs), customerId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Customer> findByDocumentNumber(String documentNumber) {

        List<Customer> result = jdbc.query(
                SELECT_CUSTOMERS + " WHERE document_number = ?",
                (rs, row) -> mapCustomer(rs), documentNumber);

        return result.stream().findFirst();
    }

    @Override
    public List<Customer> findAll() {

        return jdbc.query(
                SELECT_CUSTOMERS + " ORDER BY full_name",
                (rs, row) -> mapCustomer(rs));
    }

    @Override
    public boolean existsByDocumentNumber(String documentNumber) {

        String sql = "SELECT COUNT(*) FROM [dbo].[Customer] WHERE document_number = ?";

        Integer count = jdbc.queryForObject(sql, Integer.class, documentNumber);

        return count != null && count > 0;
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {

        return new Customer(
                rs.getLong("id"),
                rs.getString("document_number"),
                rs.getString("document_type"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                "Active".equalsIgnoreCase(rs.getString("record_status"))
        );
    }
}