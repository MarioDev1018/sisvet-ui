package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {

    Optional<Role> findById(Long roleId);

    List<Role> findAll();
}