package com.donkan.sisvetclinic.modules.commercial.service.dto.response;

public record AuthResult(
    Long id,
    String fullName,
    String username,
    String role
) {
}