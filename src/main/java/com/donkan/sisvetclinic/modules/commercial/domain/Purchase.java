package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Purchase {

    private Long id;
    private Long userId;
    private Long supplierId;
    private Long paymentMethodId;
    private LocalDateTime purchaseDatetime;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String recordStatus = "Active";
    private String operationKey;
    private String requestFingerprint;
    private List<PurchaseDetail> details = new ArrayList<>();

    public Purchase() {
    }

    public void calculateTotal() {

        totalAmount = BigDecimal.ZERO;

        for (PurchaseDetail detail : details) {
            detail.calculateSubtotal();
            totalAmount = totalAmount.add(detail.getSubtotal());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public LocalDateTime getPurchaseDatetime() {
        return purchaseDatetime;
    }

    public void setPurchaseDatetime(LocalDateTime purchaseDatetime) {
        this.purchaseDatetime = purchaseDatetime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
    }

    public String getOperationKey() {
        return operationKey;
    }

    public void setOperationKey(String operationKey) {
        this.operationKey = operationKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public List<PurchaseDetail> getDetails() {
        return details;
    }

    public void setDetails(List<PurchaseDetail> details) {
        this.details = details == null ? new ArrayList<>() : new ArrayList<>(details);
    }
}