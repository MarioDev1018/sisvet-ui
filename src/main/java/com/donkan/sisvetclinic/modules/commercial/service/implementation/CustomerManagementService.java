package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Customer;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CustomerRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.CustomerManagementUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CustomerRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;

public class CustomerManagementService implements CustomerManagementUseCase {

    private final CustomerRepositoryPort customers;

    public CustomerManagementService(CustomerRepositoryPort customers) {
        this.customers = customers;
    }

    @Override
    @Transactional
    public Customer registerCustomer(CustomerRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Los datos del cliente son obligatorios.");
        }

        String documentNumber = CommercialValidation.requireText(request.documentNumber(), "El documento");
        String fullName = CommercialValidation.requireText(request.fullName(), "El nombre");

        String documentType = request.documentType();

        if (documentType == null || documentType.isBlank()) {
            documentType = "DNI";
        }

        documentType = documentType.trim().toUpperCase();

        if (customers.existsByDocumentNumber(documentNumber)) {
            throw new IllegalStateException("Ya existe un cliente con ese documento.");
        }

        Customer customer = new Customer();

        customer.setDocumentNumber(documentNumber);
        customer.setDocumentType(documentType);
        customer.setFullName(fullName);
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setActive(true);

        return customers.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findByDni(String dni) {

        String documentNumber = CommercialValidation.requireText(dni, "El DNI");

        return customers.findByDocumentNumber(documentNumber)
                .filter(Customer::isActive)
                .orElseThrow(() -> new NoSuchElementException("El cliente no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customers.findAll().stream().filter(Customer::isActive).toList();
    }
}