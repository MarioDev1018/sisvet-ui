package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDate;

public record Pet(
    long id,
    long ownerId,
    String name,
    String species,
    String breed,
    String sex,
    LocalDate birthDate,
    Integer estimatedAgeMonths
) {}
