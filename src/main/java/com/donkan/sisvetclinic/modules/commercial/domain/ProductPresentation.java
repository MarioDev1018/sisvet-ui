package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public class ProductPresentation {

    private Long id;
    private Long productId;
    private String name;
    private int quantityInBase;
    private BigDecimal salePrice;
    private boolean defaultPresentation;
    private boolean fractionable;
    private boolean active;

    public ProductPresentation() {
    }

    public ProductPresentation(Long id, Long productId, String name, int quantityInBase,
                               BigDecimal salePrice, boolean defaultPresentation,
                               boolean fractionable, boolean active) {
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.quantityInBase = quantityInBase;
        this.salePrice = salePrice;
        this.defaultPresentation = defaultPresentation;
        this.fractionable = fractionable;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantityInBase() {
        return quantityInBase;
    }

    public void setQuantityInBase(int quantityInBase) {
        this.quantityInBase = quantityInBase;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public boolean isDefaultPresentation() {
        return defaultPresentation;
    }

    public void setDefaultPresentation(boolean defaultPresentation) {
        this.defaultPresentation = defaultPresentation;
    }

    public boolean isFractionable() {
        return fractionable;
    }

    public void setFractionable(boolean fractionable) {
        this.fractionable = fractionable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}