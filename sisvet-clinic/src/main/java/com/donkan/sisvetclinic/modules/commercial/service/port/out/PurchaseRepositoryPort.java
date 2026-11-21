package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Purchase;
import com.donkan.sisvetclinic.modules.commercial.domain.PurchaseDetail;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepositoryPort {

    Purchase save(Purchase purchase);

    void saveDetails(Long purchaseId, List<PurchaseDetail> details);

    Optional<Purchase> findById(Long purchaseId);

    Optional<Purchase> findByOperationKey(String operationKey);
}