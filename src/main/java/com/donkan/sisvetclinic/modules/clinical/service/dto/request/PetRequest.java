package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.time.LocalDate;

public record PetRequest(
    long ownerId,
    String name,
    String species,
    String breed,
    String sex,
    LocalDate birthDate,
    Integer estimatedAgeMonths
) {}
