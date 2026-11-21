package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.Role;
import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.UserRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.UserManagementUseCase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UserController {

    private final UserManagementUseCase users;

    public UserController(UserManagementUseCase users) {
        this.users = users;
    }

    @GetMapping
    public List<User> listUsers() {
        return users.listUsers();
    }

    @GetMapping("/roles")
    public List<Role> listRoles() {
        return users.listRoles();
    }

    @GetMapping("/search")
    public User findByUsername(@RequestParam String username) {
        return users.findByUsername(username);
    }

    @PostMapping
    public ResponseEntity<User> registerUser(@RequestBody UserRequest request) {

        User user = users.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{userId}")
    public User updateUser(@PathVariable Long userId, @RequestBody UserRequest request) {
        return users.updateUser(userId, request);
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {

        users.deactivateUser(userId);

        return ResponseEntity.noContent().build();
    }
}