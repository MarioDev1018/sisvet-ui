package com.donkan.sisvetclinic.modules.clinical.service.port.out;

import java.math.BigDecimal;

public interface InventoryDeductionPort {

    VaccineSupplyUsage deductForVaccination(long productId, int quantity);

    record VaccineSupplyUsage(long productId, String productName, String baseUnit, int quantity, BigDecimal supplyAmount) {}
}
