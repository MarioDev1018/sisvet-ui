package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.math.BigDecimal;

public interface ClinicalChargePort {
    boolean isSettled(long consultationId);

    default boolean isSettledByAttention(long attentionId) {
        return true;
    }

    default void registerCharge(long consultationId, String description, BigDecimal amount) {
    }

    default void registerAttentionCharge(long attentionId, String description, BigDecimal amount) {
    }
}
