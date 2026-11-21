package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {

    private Long id;
    private Long userId;
    private Long customerId;
    private Long paymentMethodId;
    private Long consultationId;
    private LocalDateTime saleDatetime;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal costTotal;
    private BigDecimal grossMargin;
    private String recordStatus = "Active";
    private String operationType = "PRODUCT";
    private String operationKey;
    private String requestFingerprint;
    private List<SaleDetail> details = new ArrayList<>();

    public Sale() {
    }

    public void calculateTotal() {

        totalAmount = BigDecimal.ZERO;

        for (SaleDetail detail : details) {
            detail.calculateSubtotal();
            totalAmount = totalAmount.add(detail.getSubtotal());
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public LocalDateTime getSaleDatetime() { return saleDatetime; }
    public void setSaleDatetime(LocalDateTime saleDatetime) { this.saleDatetime = saleDatetime; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getCostTotal() { return costTotal; }
    public void setCostTotal(BigDecimal costTotal) { this.costTotal = costTotal; }

    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }

    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }

    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public List<SaleDetail> getDetails() { return details; }

    public void setDetails(List<SaleDetail> details) {
        this.details = details == null ? new ArrayList<>() : new ArrayList<>(details);
    }
}