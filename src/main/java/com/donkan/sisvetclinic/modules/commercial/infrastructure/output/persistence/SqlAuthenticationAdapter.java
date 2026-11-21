package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.AuthenticationRepositoryPort;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlAuthenticationAdapter implements AuthenticationRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public SqlAuthenticationAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {

        String sql = "EXEC [dbo].[SP_Auth_User] ?, ?";

        List<User> users = jdbcTemplate.query(sql, (result, row) -> {

            Role role = new Role(
                    result.getLong("role_id"),
                    result.getString("role_name")
            );

            return new User(
                    result.getLong("id"),
                    result.getString("full_name"),
                    result.getString("username"),
                    role,
                    "Active".equalsIgnoreCase(result.getString("record_status"))
            );

        }, username, password);

        return users.stream().findFirst();
    }
}