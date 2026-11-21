package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.commercial;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.OwnerLookupPort;

@Repository
public class CommercialOwnerAdapter implements OwnerLookupPort {

    private final JdbcTemplate jdbc;

    public CommercialOwnerAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsOwner(long id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.Customer " +
            "WHERE id=? AND record_status='Active'",
            Integer.class, id
        );

        return count != null && count > 0;
    }
}
