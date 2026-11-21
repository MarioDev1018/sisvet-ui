package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.UserRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.UserManagementUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.RoleRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.UserRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;

public class UserManagementService implements UserManagementUseCase {

    private final UserRepositoryPort users;
    private final RoleRepositoryPort roles;

    public UserManagementService(UserRepositoryPort users, RoleRepositoryPort roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    @Transactional
    public User registerUser(UserRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios.");
        }

        String fullName = CommercialValidation.requireText(request.fullName(), "El nombre");
        String username = CommercialValidation.requireText(request.username(), "El usuario");
        String password = CommercialValidation.requireText(request.password(), "La contraseña");
        Long roleId = CommercialValidation.requireId(request.roleId(), "Rol");

        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos ocho caracteres.");
        }

        if (users.existsByUsername(username, null)) {
            throw new IllegalStateException("El nombre de usuario ya está registrado.");
        }

        Role role = roles.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("El rol no existe."));

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setRole(role);
        user.setActive(true);

        return users.create(user, password);
    }

    @Override
    @Transactional
    public User updateUser(Long userId, UserRequest request) {

        CommercialValidation.requireId(userId, "Usuario");

        if (request == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios.");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe."));

        if (!user.isActive()) {
            throw new IllegalStateException("No se puede modificar un usuario inactivo.");
        }

        String fullName = CommercialValidation.requireText(request.fullName(), "El nombre");
        String username = CommercialValidation.requireText(request.username(), "El usuario");
        Long roleId = CommercialValidation.requireId(request.roleId(), "Rol");

        if (users.existsByUsername(username, userId)) {
            throw new IllegalStateException("El nombre de usuario ya está registrado.");
        }

        Role role = roles.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("El rol no existe."));

        String newPassword = request.password();

        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos ocho caracteres.");
        }

        if (newPassword != null && newPassword.isBlank()) {
            newPassword = null;
        }

        user.setFullName(fullName);
        user.setUsername(username);
        user.setRole(role);

        return users.update(user, newPassword);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {

        CommercialValidation.requireId(userId, "Usuario");

        User user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe."));

        if (!user.isActive()) {
            throw new IllegalStateException("El usuario ya está inactivo.");
        }

        users.deactivate(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {

        String value = CommercialValidation.requireText(username, "El usuario");

        return users.findByUsername(value)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return users.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roles.findAll();
    }
}