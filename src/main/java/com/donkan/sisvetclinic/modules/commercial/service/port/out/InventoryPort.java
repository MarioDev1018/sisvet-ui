package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.InventoryDeduction;

import java.math.BigDecimal;

public interface InventoryPort {

    InventoryDeduction deductStock(Long productId, int baseQuantity);

    void increaseStock(Long productId, int baseQuantity, BigDecimal totalCost);
}