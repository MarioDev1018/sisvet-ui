package com.donkan.sisvetclinic.modules.clinical.domain;

import java.time.LocalDateTime;

public record Referral(
    long id,
    long consultationId,
    String reason,
    String destination,
    LocalDateTime createdAt,
    long authorId
) {}
