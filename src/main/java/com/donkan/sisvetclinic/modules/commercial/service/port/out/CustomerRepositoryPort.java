package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByDocumentNumber(String documentNumber);

    List<Customer> findAll();

    boolean existsByDocumentNumber(String documentNumber);
}