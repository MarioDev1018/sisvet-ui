package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.Customer;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CustomerRequest;

import java.util.List;

public interface CustomerManagementUseCase {

    Customer registerCustomer(CustomerRequest request);

    Customer findByDni(String dni);

    List<Customer> listCustomers();
}