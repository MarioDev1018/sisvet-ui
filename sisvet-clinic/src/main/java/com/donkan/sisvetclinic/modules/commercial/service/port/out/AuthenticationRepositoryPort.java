package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.User;
import java.util.Optional;

public interface AuthenticationRepositoryPort {

    Optional<User> authenticate(String username, String password);
}