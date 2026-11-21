package com.donkan.sisvetclinic.modules.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Expense {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private BigDecimal amount;
    private String category;
    private LocalDateTime expenseDatetime;
    private boolean automatic;
    private String recurringCode;
    private LocalDate billingPeriod;
    private String recordStatus = "Active";

    public Expense() {
    }

    public boolean isValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(recordStatus);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getExpenseDatetime() {
        return expenseDatetime;
    }

    public void setExpenseDatetime(LocalDateTime expenseDatetime) {
        this.expenseDatetime = expenseDatetime;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public String getRecurringCode() {
        return recurringCode;
    }

    public void setRecurringCode(String recurringCode) {
        this.recurringCode = recurringCode;
    }

    public LocalDate getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(LocalDate billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
    }
}