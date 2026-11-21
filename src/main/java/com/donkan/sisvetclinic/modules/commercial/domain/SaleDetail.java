package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public class SaleDetail {

    private Long id;
    private Long saleId;
    private Long productId;
    private Long presentationId;
    private int quantity;
    private int baseQuantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal costTotal;

    public SaleDetail() {
    }

    public SaleDetail(Long id, Long saleId, Long productId,
                      Long presentationId, int quantity, int baseQuantity,
                      BigDecimal unitPrice) {

        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.presentationId = presentationId;
        this.quantity = quantity;
        this.baseQuantity = baseQuantity;
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public void calculateSubtotal() {

        if (unitPrice == null || unitPrice.signum() <= 0
                || quantity <= 0 || baseQuantity <= 0) {
            throw new IllegalStateException("Detalle de venta inválido.");
        }

        subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getGrossMargin() {

        if (subtotal == null || costTotal == null) {
            return null;
        }

        return subtotal.subtract(costTotal);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getPresentationId() { return presentationId; }
    public void setPresentationId(Long presentationId) { this.presentationId = presentationId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getBaseQuantity() { return baseQuantity; }
    public void setBaseQuantity(int baseQuantity) { this.baseQuantity = baseQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getCostTotal() { return costTotal; }
    public void setCostTotal(BigDecimal costTotal) { this.costTotal = costTotal; }
}