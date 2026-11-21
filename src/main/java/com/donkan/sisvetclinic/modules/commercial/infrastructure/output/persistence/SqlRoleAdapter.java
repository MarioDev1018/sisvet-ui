package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.RoleRepositoryPort;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlRoleAdapter implements RoleRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlRoleAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Role> findById(Long roleId) {

        String sql = "SELECT id, name FROM [dbo].[Role] WHERE id = ?";

        List<Role> result = jdbc.query(sql, (rs, row) ->
                new Role(rs.getLong("id"), rs.getString("name")), roleId);

        return result.stream().findFirst();
    }

    @Override
    public List<Role> findAll() {

        String sql = "SELECT id, name FROM [dbo].[Role] ORDER BY name";

        return jdbc.query(sql, (rs, row) ->
                new Role(rs.getLong("id"), rs.getString("name")));
    }
}