package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.domain.SaleDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepositoryPort {

    Sale save(Sale sale);

    void saveDetails(Long saleId, List<SaleDetail> details);

    Optional<Sale> findById(Long saleId);

    Optional<Sale> findByOperationKey(String operationKey);

    List<SaleDetail> findDetailsBySaleId(Long saleId);

    List<Sale> findAll();

    List<Sale> findByPeriod(LocalDate startDate, LocalDate endDate);
}