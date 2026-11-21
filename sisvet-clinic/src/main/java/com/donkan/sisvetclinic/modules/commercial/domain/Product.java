package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public class Product {

    private Long id;
    private String code;
    private String name;
    private BigDecimal price;
    private int stock;
    private Long categoryId;
    private boolean active;
    private String baseUnit = "UNIT";
    private BigDecimal averageUnitCost;

    public Product() {
    }

    public Product(Long id, String code, String name, BigDecimal price,
                   int stock, Long categoryId, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }

    public BigDecimal getAverageUnitCost() { return averageUnitCost; }
    public void setAverageUnitCost(BigDecimal averageUnitCost) {
        this.averageUnitCost = averageUnitCost;
    }
}