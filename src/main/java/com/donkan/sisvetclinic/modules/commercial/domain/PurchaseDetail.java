package com.donkan.sisvetclinic.modules.commercial.domain;

import java.math.BigDecimal;

public class PurchaseDetail {

    private Long id;
    private Long purchaseId;
    private Long productId;
    private Long presentationId;
    private int quantity;
    private int baseQuantity;
    private BigDecimal unitCost;
    private BigDecimal subtotal;

    public PurchaseDetail() {
    }

    public PurchaseDetail(Long id, Long purchaseId, Long productId,
                          Long presentationId, int quantity, int baseQuantity,
                          BigDecimal unitCost) {
        this.id = id;
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.presentationId = presentationId;
        this.quantity = quantity;
        this.baseQuantity = baseQuantity;
        this.unitCost = unitCost;
        calculateSubtotal();
    }

    public void calculateSubtotal() {

        if (unitCost == null || unitCost.signum() <= 0
                || quantity <= 0 || baseQuantity <= 0) {
            throw new IllegalStateException("Detalle de compra inválido.");
        }

        subtotal = unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getPresentationId() { return presentationId; }
    public void setPresentationId(Long presentationId) { this.presentationId = presentationId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getBaseQuantity() { return baseQuantity; }
    public void setBaseQuantity(int baseQuantity) { this.baseQuantity = baseQuantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}