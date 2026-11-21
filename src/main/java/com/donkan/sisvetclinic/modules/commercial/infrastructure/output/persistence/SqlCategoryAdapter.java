package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Category;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CategoryRepositoryPort;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlCategoryAdapter implements CategoryRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlCategoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Category> findById(Long categoryId) {

        String sql = """
            SELECT id, name FROM [dbo].[Category]
            WHERE id = ? AND record_status = 'Active'
            """;

        List<Category> result = jdbc.query(sql, (rs, row) ->
                new Category(rs.getLong("id"), rs.getString("name")), categoryId);

        return result.stream().findFirst();
    }

    @Override
    public List<Category> findAll() {

        String sql = """
            SELECT id, name FROM [dbo].[Category]
            WHERE record_status = 'Active'
            ORDER BY name
            """;

        return jdbc.query(sql, (rs, row) ->
                new Category(rs.getLong("id"), rs.getString("name")));
    }
}