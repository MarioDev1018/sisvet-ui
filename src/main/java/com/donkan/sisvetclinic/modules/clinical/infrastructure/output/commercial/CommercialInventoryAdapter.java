package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.commercial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.donkan.sisvetclinic.modules.clinical.service.port.out.InventoryDeductionPort;

@Repository
public class CommercialInventoryAdapter implements InventoryDeductionPort {

    private final JdbcTemplate jdbc;

    public CommercialInventoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public VaccineSupplyUsage deductForVaccination(long productId, int quantity) {
        if (productId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("Producto y cantidad de dosis deben ser mayores a cero.");
        }

        List<VaccineProductRow> rows = jdbc.query(
            """
            SELECT p.id, p.name, p.base_unit, p.price,
                   pp.sale_price, pp.quantity_in_base
            FROM dbo.Product p
            LEFT JOIN dbo.ProductPresentation pp
                   ON pp.product_id = p.id AND pp.is_default = 1 AND pp.is_active = 1
            WHERE p.id = ? AND p.record_status = 'Active'
            """,
            (rs, n) -> new VaccineProductRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("base_unit"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("sale_price"),
                (Integer) rs.getObject("quantity_in_base")
            ),
            productId
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("El insumo/vacuna seleccionado no existe o está inactivo.");
        }

        VaccineProductRow item = rows.get(0);

        int affected = jdbc.update(
            """
            UPDATE dbo.Product
            SET stock_quantity = stock_quantity - ?
            WHERE id = ?
              AND stock_quantity - reserved_quantity >= ?
              AND record_status = 'Active'
            """,
            quantity, productId, quantity
        );

        if (affected != 1) {
            throw new IllegalStateException("Stock insuficiente en inventario para \"" + item.name() + "\" (requiere " + quantity + " " + item.baseUnit() + ").");
        }

        BigDecimal basePrice = item.salePrice() != null ? item.salePrice() : item.productPrice();
        int divisor = item.quantityInBase() != null && item.quantityInBase() > 0 ? item.quantityInBase() : 1;
        BigDecimal unitPrice = basePrice == null
                ? BigDecimal.ZERO
                : basePrice.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP);
        BigDecimal supplyAmount = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        return new VaccineSupplyUsage(
            item.id(),
            item.name(),
            item.baseUnit() == null ? "UNIT" : item.baseUnit(),
            quantity,
            supplyAmount
        );
    }

    private record VaccineProductRow(
        long id,
        String name,
        String baseUnit,
        BigDecimal productPrice,
        BigDecimal salePrice,
        Integer quantityInBase
    ) {}
}
