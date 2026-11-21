package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.donkan.sisvetclinic.modules.clinical.domain.Actor;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.ActorPort;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;

@Repository
public class SqlActorAdapter implements ActorPort {

    private final JdbcTemplate jdbc;

    public SqlActorAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Actor current() {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
            "anonymousUser".equals(auth.getPrincipal())) {
            throw new SecurityException("Debe iniciar sesión");
        }

        String username = auth.getPrincipal() instanceof AuthResult authResult
            ? authResult.username()
            : auth.getName();

        return jdbc.query(
            "SELECT u.id, r.name AS role_name " +
            "FROM dbo.[User] u JOIN dbo.Role r ON r.id=u.role_id " +
            "WHERE u.username=? AND u.record_status='Active'",
            (r, n) -> new Actor(
                r.getLong("id"), r.getString("role_name")
            ),
            username
        ).stream().findFirst().orElseThrow(
            () -> new SecurityException(
                "Cuenta sin permisos vigentes"
            )
        );
    }
}
