package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.InventoryDeduction;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.InventoryPort;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlInventoryAdapter implements InventoryPort {

    private final JdbcTemplate jdbc;

    public SqlInventoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InventoryDeduction deductStock(Long productId, int baseQuantity) {

        if (productId == null || productId <= 0 || baseQuantity <= 0) {
            throw new IllegalArgumentException("Producto o cantidad inválida.");
        }

        String sql = """
            UPDATE [dbo].[Product]
            SET stock_quantity = stock_quantity - ?
            OUTPUT DELETED.average_unit_cost
            WHERE id = ?
              AND stock_quantity >= ?
              AND record_status = 'Active'
            """;

        List<BigDecimal> costs = jdbc.query(
                sql,
                (rs, row) -> rs.getBigDecimal(1),
                baseQuantity,
                productId,
                baseQuantity
        );

        if (costs.isEmpty()) {
            return new InventoryDeduction(false, null);
        }

        return new InventoryDeduction(true, costs.get(0));
    }

    @Override
    public void increaseStock(Long productId, int baseQuantity, BigDecimal totalCost) {

        if (productId == null || productId <= 0 || baseQuantity <= 0
                || totalCost == null || totalCost.signum() <= 0) {
            throw new IllegalArgumentException("Datos de entrada de inventario inválidos.");
        }

        String sql = """
            UPDATE [dbo].[Product]
            SET
                average_unit_cost =
                    CASE
                        WHEN stock_quantity = 0 THEN
                            CAST(
                                CAST(? AS DECIMAL(18,6)) / ?
                                AS DECIMAL(18,6)
                            )
                        WHEN average_unit_cost IS NULL THEN NULL
                        ELSE
                            CAST(
                                (
                                    average_unit_cost * stock_quantity
                                    + CAST(? AS DECIMAL(18,6))
                                ) / (stock_quantity + ?)
                                AS DECIMAL(18,6)
                            )
                    END,
                stock_quantity = stock_quantity + ?
            WHERE id = ?
              AND record_status = 'Active'
              AND stock_quantity <= 2147483647 - ?
            """;

        int updated = jdbc.update(sql,
                totalCost,
                baseQuantity,
                totalCost,
                baseQuantity,
                baseQuantity,
                productId,
                baseQuantity
        );

        if (updated != 1) {
            throw new IllegalStateException("No se pudo incrementar el inventario.");
        }
    }
}