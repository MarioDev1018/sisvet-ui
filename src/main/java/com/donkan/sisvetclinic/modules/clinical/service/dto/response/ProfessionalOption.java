package com.donkan.sisvetclinic.modules.clinical.service.dto.response;

import java.util.List;

public record ProfessionalOption(
    long id,
    String fullName,
    String roleName,
    List<String> specialties
) {}
