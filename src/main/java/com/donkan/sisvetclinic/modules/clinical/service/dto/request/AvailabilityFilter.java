package com.donkan.sisvetclinic.modules.clinical.service.dto.request;

import java.time.LocalDateTime;

public record AvailabilityFilter(
    long professionalId,
    LocalDateTime start,
    LocalDateTime end
) {}
