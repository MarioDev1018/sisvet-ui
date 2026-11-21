package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record Treatment(
    long id,
    long consultationId,
    String description,
    String instructions,
    Long productId,
    LocalDateTime prescribedAt,
    long authorId
) {}
