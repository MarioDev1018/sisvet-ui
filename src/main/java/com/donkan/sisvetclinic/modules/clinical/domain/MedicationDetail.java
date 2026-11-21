package com.donkan.sisvetclinic.modules.clinical.domain;

public record MedicationDetail(
    long productId,
    String tradeName,
    String activeIngredient,
    String concentration,
    String presentation
) {}
