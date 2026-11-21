package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.domain.SaleDetail;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SaleRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlSaleAdapter implements SaleRepositoryPort {

    private final JdbcTemplate jdbc;

    private static final String SELECT_SALES = """
        SELECT id, user_id, customer_id, payment_method_id, consultation_id,
               sale_datetime, total_amount, cost_total, gross_margin,
               record_status, operation_type, operation_key, request_fingerprint
        FROM [dbo].[Sale]
        """;

    public SqlSaleAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Sale save(Sale sale) {

        String sql = """
            INSERT INTO [dbo].[Sale] (
                user_id, customer_id, payment_method_id, consultation_id,
                sale_datetime, total_amount, cost_total, gross_margin,
                record_status, operation_type, operation_key, request_fingerprint
            )
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                sale.getUserId(),
                sale.getCustomerId(),
                sale.getPaymentMethodId(),
                sale.getConsultationId(),
                Timestamp.valueOf(sale.getSaleDatetime()),
                sale.getTotalAmount(),
                sale.getCostTotal(),
                sale.getGrossMargin(),
                sale.getRecordStatus(),
                sale.getOperationType(),
                sale.getOperationKey(),
                sale.getRequestFingerprint());

        sale.setId(id);

        return sale;
    }

    @Override
    public void saveDetails(Long saleId, List<SaleDetail> details) {

        String sql = """
            INSERT INTO [dbo].[SaleDetail] (
                sale_id, product_id, presentation_id,
                quantity, base_quantity, unit_price, subtotal, cost_total
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        for (SaleDetail detail : details) {

            jdbc.update(sql,
                    saleId,
                    detail.getProductId(),
                    detail.getPresentationId(),
                    detail.getQuantity(),
                    detail.getBaseQuantity(),
                    detail.getUnitPrice(),
                    detail.getSubtotal(),
                    detail.getCostTotal());
        }
    }

    @Override
    public Optional<Sale> findById(Long saleId) {

        List<Sale> result = jdbc.query(
                SELECT_SALES + " WHERE id = ?",
                (rs, row) -> mapSale(rs),
                saleId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Sale> findByOperationKey(String operationKey) {

        List<Sale> result = jdbc.query(
                SELECT_SALES + " WHERE operation_key = ?",
                (rs, row) -> mapSale(rs),
                operationKey);

        return result.stream().findFirst();
    }

    @Override
    public List<SaleDetail> findDetailsBySaleId(Long saleId) {

        String sql = """
            SELECT id, sale_id, product_id, presentation_id,
                   quantity, base_quantity, unit_price, subtotal, cost_total
            FROM [dbo].[SaleDetail]
            WHERE sale_id = ?
            ORDER BY id
            """;

        return jdbc.query(sql, (rs, row) -> mapDetail(rs), saleId);
    }

    @Override
    public List<Sale> findAll() {

        return jdbc.query(
                SELECT_SALES + " ORDER BY sale_datetime DESC, id DESC",
                (rs, row) -> mapSale(rs));
    }

    @Override
    public List<Sale> findByPeriod(LocalDate startDate, LocalDate endDate) {

        String sql = SELECT_SALES + """
            WHERE sale_datetime >= ?
              AND sale_datetime < ?
            ORDER BY sale_datetime DESC, id DESC
            """;

        return jdbc.query(sql, (rs, row) -> mapSale(rs),
                Timestamp.valueOf(startDate.atStartOfDay()),
                Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
    }

    private Sale mapSale(ResultSet rs) throws SQLException {

        Sale sale = new Sale();

        sale.setId(rs.getLong("id"));
        sale.setUserId(rs.getLong("user_id"));
        sale.setCustomerId(rs.getLong("customer_id"));
        sale.setPaymentMethodId(rs.getLong("payment_method_id"));

        long consultationId = rs.getLong("consultation_id");

        if (!rs.wasNull()) {
            sale.setConsultationId(consultationId);
        }

        sale.setSaleDatetime(rs.getTimestamp("sale_datetime").toLocalDateTime());
        sale.setTotalAmount(rs.getBigDecimal("total_amount"));
        sale.setCostTotal(rs.getBigDecimal("cost_total"));
        sale.setGrossMargin(rs.getBigDecimal("gross_margin"));
        sale.setRecordStatus(rs.getString("record_status"));
        sale.setOperationType(rs.getString("operation_type"));
        sale.setOperationKey(rs.getString("operation_key"));
        sale.setRequestFingerprint(rs.getString("request_fingerprint"));

        return sale;
    }

    private SaleDetail mapDetail(ResultSet rs) throws SQLException {

        SaleDetail detail = new SaleDetail();

        detail.setId(rs.getLong("id"));
        detail.setSaleId(rs.getLong("sale_id"));
        detail.setProductId(rs.getLong("product_id"));
        detail.setPresentationId(rs.getLong("presentation_id"));
        detail.setQuantity(rs.getInt("quantity"));
        detail.setBaseQuantity(rs.getInt("base_quantity"));
        detail.setUnitPrice(rs.getBigDecimal("unit_price"));
        detail.setSubtotal(rs.getBigDecimal("subtotal"));
        detail.setCostTotal(rs.getBigDecimal("cost_total"));

        return detail;
    }
}