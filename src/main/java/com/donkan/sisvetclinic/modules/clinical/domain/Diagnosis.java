package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record Diagnosis(
    long id,
    long consultationId,
    String description,
    String indications,
    long authorId,
    LocalDateTime createdAt
) {}
