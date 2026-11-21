package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.User;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.LoginRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.AuthenticationUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.AuthenticationRepositoryPort;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final AuthenticationRepositoryPort authenticationRepository;

    public AuthenticationService(AuthenticationRepositoryPort authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public AuthResult login(LoginRequest request) {

        if (request == null || request.username() == null || request.password() == null
                || request.username().isBlank() || request.password().isBlank()) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        User user = authenticationRepository.authenticate(request.username(), request.password())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas."));

        if (!user.isActive()) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        return new AuthResult(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole().getName()
        );
    }
}