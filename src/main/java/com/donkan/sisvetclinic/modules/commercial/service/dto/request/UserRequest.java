package com.donkan.sisvetclinic.modules.commercial.service.dto.request;

public record UserRequest(
        String fullName,
        String username,
        String password,
        Long roleId
) {
}