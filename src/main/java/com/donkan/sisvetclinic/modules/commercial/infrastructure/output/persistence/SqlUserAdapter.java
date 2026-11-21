package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.UserRepositoryPort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlUserAdapter implements UserRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlUserAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_USERS = """
            SELECT u.id, u.full_name, u.username, u.record_status,
                   r.id AS role_id, r.name AS role_name
            FROM [dbo].[User] u
            INNER JOIN [dbo].[Role] r ON r.id = u.role_id
            """;

    @Override
    public User create(User user, String password) {

        validatePassword(password);

        String salt = UUID.randomUUID().toString();

        String sql = """
            INSERT INTO [dbo].[User]
                (full_name, username, password_hash, password_salt, role_id, record_status)
            OUTPUT INSERTED.id
            VALUES (
                ?, ?,
                HASHBYTES('SHA2_512',
                    CAST(? AS NVARCHAR(100)) +
                    CAST(CONVERT(UNIQUEIDENTIFIER, ?) AS NVARCHAR(36))
                ),
                CONVERT(UNIQUEIDENTIFIER, ?),
                ?,
                'Active'
            )
            """;

        Long id = jdbc.queryForObject(sql, Long.class,
                user.getFullName(), user.getUsername(), password,
                salt, salt, user.getRole().getId());

        return findById(id).orElseThrow();
    }

    @Override
    public User update(User user, String newPassword) {

        String sql;
        int rows;

        if (newPassword == null || newPassword.isBlank()) {

            sql = """
                UPDATE [dbo].[User]
                SET full_name = ?, username = ?, role_id = ?
                WHERE id = ? AND record_status = 'Active'
                """;

            rows = jdbc.update(sql, user.getFullName(), user.getUsername(),
                    user.getRole().getId(), user.getId());

        } else {

            validatePassword(newPassword);

            String salt = UUID.randomUUID().toString();

            sql = """
                UPDATE [dbo].[User]
                SET full_name = ?,
                    username = ?,
                    role_id = ?,
                    password_hash = HASHBYTES('SHA2_512',
                        CAST(? AS NVARCHAR(100)) +
                        CAST(CONVERT(UNIQUEIDENTIFIER, ?) AS NVARCHAR(36))
                    ),
                    password_salt = CONVERT(UNIQUEIDENTIFIER, ?)
                WHERE id = ? AND record_status = 'Active'
                """;

            rows = jdbc.update(sql, user.getFullName(), user.getUsername(),
                    user.getRole().getId(), newPassword, salt, salt, user.getId());
        }

        if (rows != 1) {
            throw new NoSuchElementException("El usuario no existe o está inactivo.");
        }

        return findById(user.getId()).orElseThrow();
    }

    @Override
    public void deactivate(Long userId) {

        String sql = """
            UPDATE [dbo].[User]
            SET record_status = 'Inactive'
            WHERE id = ? AND record_status = 'Active'
            """;

        if (jdbc.update(sql, userId) != 1) {
            throw new NoSuchElementException("El usuario no existe o ya está inactivo.");
        }
    }

    @Override
    public Optional<User> findById(Long userId) {

        List<User> result = jdbc.query(
                SELECT_USERS + " WHERE u.id = ?",
                (rs, row) -> mapUser(rs), userId);

        return result.stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {

        List<User> result = jdbc.query(
                SELECT_USERS + " WHERE u.username = ?",
                (rs, row) -> mapUser(rs), username);

        return result.stream().findFirst();
    }

    @Override
    public List<User> findAll() {

        return jdbc.query(
                SELECT_USERS + " ORDER BY u.full_name",
                (rs, row) -> mapUser(rs));
    }

    @Override
    public boolean existsByUsername(String username, Long excludedUserId) {

        String sql = "SELECT COUNT(*) FROM [dbo].[User] WHERE username = ?";

        Integer count;

        if (excludedUserId == null) {
            count = jdbc.queryForObject(sql, Integer.class, username);
        } else {
            count = jdbc.queryForObject(sql + " AND id <> ?", Integer.class,
                    username, excludedUserId);
        }

        return count != null && count > 0;
    }

    private User mapUser(ResultSet rs) throws SQLException {

        Role role = new Role(
                rs.getLong("role_id"),
                rs.getString("role_name")
        );

        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("username"),
                role,
                "Active".equalsIgnoreCase(rs.getString("record_status"))
        );
    }

    private void validatePassword(String password) {

        if (password == null || password.length() < 8 || password.length() > 100) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 100 caracteres.");
        }
    }
}