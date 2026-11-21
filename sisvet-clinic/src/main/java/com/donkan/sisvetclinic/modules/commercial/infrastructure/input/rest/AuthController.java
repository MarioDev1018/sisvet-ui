package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.LoginRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.AuthenticationUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthenticationUseCase authenticationUseCase,
                          SecurityContextRepository securityContextRepository,
                          CsrfTokenRepository csrfTokenRepository) {

        this.authenticationUseCase = authenticationUseCase;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {

        try {

            AuthResult result = authenticationUseCase.login(request);

            String authority = normalizeRole(result.role());

            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                    result,
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            );

            httpRequest.getSession(true);
            httpRequest.changeSessionId();

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            csrfTokenRepository.saveToken(null, httpRequest, httpResponse);

            return ResponseEntity.ok(result);

        } catch (BadCredentialsException exception) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Usuario o contraseña incorrectos."));
        }
    }

    @GetMapping("/me")
    public AuthResult currentUser(Authentication authentication) {
        return (AuthResult) authentication.getPrincipal();
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    private String normalizeRole(String role) {

        String normalized = Normalizer.normalize(role, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');

        return "ROLE_" + normalized;
    }
}