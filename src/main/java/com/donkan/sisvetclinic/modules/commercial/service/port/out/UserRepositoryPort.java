package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User create(User user, String password);

    User update(User user, String newPassword);

    void deactivate(Long userId);

    Optional<User> findById(Long userId);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    boolean existsByUsername(String username, Long excludedUserId);
}