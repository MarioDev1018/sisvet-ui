package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Invoice;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.InvoiceRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlInvoiceAdapter implements InvoiceRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlInvoiceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_INVOICES = """
            SELECT id, sale_id, series, number, status, external_reference
            FROM [dbo].[Invoice]
            """;

    @Override
    public Invoice save(Invoice invoice) {

        if (invoice.getId() == null) {

            String sql = """
                INSERT INTO [dbo].[Invoice]
                    (sale_id, series, number, status, external_reference)
                OUTPUT INSERTED.id
                VALUES (?, ?, ?, ?, ?)
                """;

            Long id = jdbc.queryForObject(sql, Long.class,
                    invoice.getSaleId(),
                    invoice.getSeries(),
                    invoice.getNumber(),
                    invoice.getStatusCode(),
                    invoice.getExternalReference());

            return findById(id).orElseThrow();
        }

        String sql = """
            UPDATE [dbo].[Invoice]
            SET series = ?,
                number = ?,
                status = ?,
                external_reference = ?
            WHERE id = ?
              AND status = 'PENDING_VERIFICATION'
            """;

        int updated = jdbc.update(sql,
                invoice.getSeries(),
                invoice.getNumber(),
                invoice.getStatusCode(),
                invoice.getExternalReference(),
                invoice.getId());

        if (updated != 1) {
            throw new IllegalStateException(
                    "El comprobante cambió de estado o ya finalizó su emisión."
            );
        }

        return findById(invoice.getId())
                .orElseThrow(() -> new NoSuchElementException("El comprobante no existe."));
    }

    @Override
    public Optional<Invoice> findById(Long invoiceId) {

        List<Invoice> result = jdbc.query(
                SELECT_INVOICES + " WHERE id = ?",
                (rs, row) -> mapInvoice(rs), invoiceId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Invoice> findBySaleId(Long saleId) {

        List<Invoice> result = jdbc.query(
                SELECT_INVOICES + " WHERE sale_id = ?",
                (rs, row) -> mapInvoice(rs), saleId);

        return result.stream().findFirst();
    }

    @Override
    public boolean tryStartEmission(Long invoiceId) {

        String sql = """
            UPDATE [dbo].[Invoice]
            SET status = 'PENDING_VERIFICATION'
            WHERE id = ?
              AND status = 'PENDING'
            """;

        return jdbc.update(sql, invoiceId) == 1;
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {

        return Invoice.restore(
                rs.getLong("id"),
                rs.getLong("sale_id"),
                rs.getString("series"),
                rs.getString("number"),
                rs.getString("external_reference"),
                rs.getString("status")
        );
    }
}