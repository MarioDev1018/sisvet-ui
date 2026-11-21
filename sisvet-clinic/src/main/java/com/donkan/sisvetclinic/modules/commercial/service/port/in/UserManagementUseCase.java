package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.UserRequest;

import java.util.List;

public interface UserManagementUseCase {

    User registerUser(UserRequest request);

    User updateUser(Long userId, UserRequest request);

    void deactivateUser(Long userId);

    User findByUsername(String username);

    List<User> listUsers();

    List<Role> listRoles();
}