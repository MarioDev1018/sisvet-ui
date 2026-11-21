package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public class ClinicalCharge {

    private Long id;
    private Long consultationId;
    private String description;
    private BigDecimal amount;
    private boolean charged;

    public ClinicalCharge() {
    }

    public ClinicalCharge(Long id, Long consultationId, String description,
                          BigDecimal amount, boolean charged) {
        this.id = id;
        this.consultationId = consultationId;
        this.description = description;
        this.amount = amount;
        this.charged = charged;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isCharged() {
        return charged;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }
}