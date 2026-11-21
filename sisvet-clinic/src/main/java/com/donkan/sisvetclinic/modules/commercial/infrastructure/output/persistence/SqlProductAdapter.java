package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Product;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.ProductRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlProductAdapter implements ProductRepositoryPort {

    private final JdbcTemplate jdbc;

    private static final String SELECT = """
        SELECT id, product_code, name, price, stock_quantity,
               category_id, record_status, base_unit, average_unit_cost
        FROM [dbo].[Product]
        """;

    public SqlProductAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Product save(Product product) {

        String sql = """
            INSERT INTO [dbo].[Product]
                (product_code, name, price, stock_quantity,
                 category_id, record_status, base_unit)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                product.getCode(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategoryId(),
                product.isActive() ? "Active" : "Inactive",
                product.getBaseUnit());

        return findById(id).orElseThrow();
    }

    @Override
    public Optional<Product> findById(Long productId) {

        List<Product> result = jdbc.query(
                SELECT + " WHERE id = ?", (rs, row) -> map(rs), productId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<Product> findByCode(String code) {

        List<Product> result = jdbc.query(
                SELECT + " WHERE product_code = ?", (rs, row) -> map(rs), code);

        return result.stream().findFirst();
    }

    @Override
    public List<Product> findAll() {

        return jdbc.query(
                SELECT + " ORDER BY name", (rs, row) -> map(rs));
    }

    @Override
    public boolean existsByCode(String code) {

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM [dbo].[Product] WHERE product_code = ?",
                Integer.class,
                code);

        return count != null && count > 0;
    }

    private Product map(ResultSet rs) throws SQLException {

        Product product = new Product(
                rs.getLong("id"),
                rs.getString("product_code"),
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_quantity"),
                rs.getLong("category_id"),
                "Active".equalsIgnoreCase(rs.getString("record_status"))
        );

        product.setBaseUnit(rs.getString("base_unit"));
        product.setAverageUnitCost(rs.getBigDecimal("average_unit_cost"));

        return product;
    }
}