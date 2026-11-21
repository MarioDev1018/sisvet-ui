package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.ProductPresentation;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.ProductPresentationRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlProductPresentationAdapter implements ProductPresentationRepositoryPort {

    private final JdbcTemplate jdbc;

    private static final String SELECT = """
        SELECT id, product_id, name, quantity_in_base, sale_price,
               is_default, is_fractionable, is_active
        FROM [dbo].[ProductPresentation]
        """;

    public SqlProductPresentationAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ProductPresentation save(ProductPresentation presentation) {

        String sql = """
            INSERT INTO [dbo].[ProductPresentation]
                (product_id, name, quantity_in_base, sale_price,
                 is_default, is_fractionable, is_active)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                presentation.getProductId(),
                presentation.getName(),
                presentation.getQuantityInBase(),
                presentation.getSalePrice(),
                presentation.isDefaultPresentation(),
                presentation.isFractionable(),
                presentation.isActive());

        return findById(id).orElseThrow();
    }

    @Override
    public Optional<ProductPresentation> findById(Long id) {

        List<ProductPresentation> result = jdbc.query(
                SELECT + " WHERE id = ?",
                (rs, row) -> map(rs), id);

        return result.stream().findFirst();
    }

    @Override
    public Optional<ProductPresentation> findDefaultByProductId(Long productId) {

        List<ProductPresentation> result = jdbc.query(
                SELECT + " WHERE product_id = ? AND is_default = 1 AND is_active = 1",
                (rs, row) -> map(rs), productId);

        return result.stream().findFirst();
    }

    @Override
    public List<ProductPresentation> findByProductId(Long productId) {

        return jdbc.query(
                SELECT + " WHERE product_id = ? AND is_active = 1 ORDER BY is_default DESC, name",
                (rs, row) -> map(rs), productId);
    }

    private ProductPresentation map(ResultSet rs) throws SQLException {

        return new ProductPresentation(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getString("name"),
                rs.getInt("quantity_in_base"),
                rs.getBigDecimal("sale_price"),
                rs.getBoolean("is_default"),
                rs.getBoolean("is_fractionable"),
                rs.getBoolean("is_active")
        );
    }
}