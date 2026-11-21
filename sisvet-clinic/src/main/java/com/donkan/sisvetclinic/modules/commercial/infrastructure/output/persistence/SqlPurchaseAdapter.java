package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Purchase;
import com.donkan.sisvetclinic.modules.commercial.domain.PurchaseDetail;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PurchaseRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlPurchaseAdapter implements PurchaseRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlPurchaseAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_PURCHASES = """
            SELECT id, user_id, supplier_id, payment_method_id,
                   purchase_datetime, total_amount, record_status,
                   operation_key, request_fingerprint
            FROM [dbo].[Purchase]
            """;

    @Override
    public Purchase save(Purchase purchase) {

        String sql = """
            INSERT INTO [dbo].[Purchase]
                (user_id, supplier_id, payment_method_id, purchase_datetime,
                 total_amount, record_status, operation_key, request_fingerprint)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                purchase.getUserId(),
                purchase.getSupplierId(),
                purchase.getPaymentMethodId(),
                Timestamp.valueOf(purchase.getPurchaseDatetime()),
                purchase.getTotalAmount(),
                purchase.getRecordStatus(),
                purchase.getOperationKey(),
                purchase.getRequestFingerprint());

        purchase.setId(id);

        return purchase;
    }

    @Override
    public void saveDetails(Long purchaseId, List<PurchaseDetail> details) {

        String sql = """
            INSERT INTO [dbo].[PurchaseDetail]
                (purchase_id, product_id, presentation_id,
                 quantity, base_quantity, unit_cost, subtotal)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        for (PurchaseDetail detail : details) {

            jdbc.update(sql,
                    purchaseId,
                    detail.getProductId(),
                    detail.getPresentationId(),
                    detail.getQuantity(),
                    detail.getBaseQuantity(),
                    detail.getUnitCost(),
                    detail.getSubtotal());
        }
    }

    @Override
    public Optional<Purchase> findById(Long purchaseId) {

        List<Purchase> result = jdbc.query(
                SELECT_PURCHASES + " WHERE id = ?",
                (rs, row) -> mapPurchase(rs), purchaseId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Purchase> findByOperationKey(String operationKey) {

        List<Purchase> result = jdbc.query(
                SELECT_PURCHASES + " WHERE operation_key = ?",
                (rs, row) -> mapPurchase(rs), operationKey);

        return result.stream().findFirst();
    }

    private Purchase mapPurchase(ResultSet rs) throws SQLException {

        Purchase purchase = new Purchase();

        purchase.setId(rs.getLong("id"));
        purchase.setUserId(rs.getLong("user_id"));
        purchase.setSupplierId(rs.getLong("supplier_id"));
        purchase.setPaymentMethodId(rs.getLong("payment_method_id"));
        purchase.setPurchaseDatetime(rs.getTimestamp("purchase_datetime").toLocalDateTime());
        purchase.setTotalAmount(rs.getBigDecimal("total_amount"));
        purchase.setRecordStatus(rs.getString("record_status"));
        purchase.setOperationKey(rs.getString("operation_key"));
        purchase.setRequestFingerprint(rs.getString("request_fingerprint"));

        return purchase;
    }
}