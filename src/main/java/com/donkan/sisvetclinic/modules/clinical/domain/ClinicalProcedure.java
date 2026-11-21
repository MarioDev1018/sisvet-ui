package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record ClinicalProcedure(
    long id,
    long consultationId,
    String name,
    String observations,
    LocalDateTime performedAt,
    long authorId
) {}
