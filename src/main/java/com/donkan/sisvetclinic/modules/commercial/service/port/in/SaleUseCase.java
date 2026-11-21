package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;
import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.domain.SaleDetail;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;

import java.time.LocalDate;
import java.util.List;

public interface SaleUseCase {

    SaleResult registerSale(Long userId, SaleRequest request);

    Sale findById(Long saleId);

    List<SaleDetail> findDetails(Long saleId);

    List<Sale> listSales();

    List<Sale> listByPeriod(LocalDate startDate, LocalDate endDate);

    List<PaymentMethod> listPaymentMethods();
}