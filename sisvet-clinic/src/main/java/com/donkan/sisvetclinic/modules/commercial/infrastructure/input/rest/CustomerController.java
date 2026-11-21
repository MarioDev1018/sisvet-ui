package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.Customer;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CustomerRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.CustomerManagementUseCase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerManagementUseCase customers;

    public CustomerController(CustomerManagementUseCase customers) {
        this.customers = customers;
    }

    @GetMapping
    public List<Customer> listCustomers() {
        return customers.listCustomers();
    }

    @GetMapping("/search")
    public Customer findByDni(@RequestParam String dni) {
        return customers.findByDni(dni);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public ResponseEntity<Customer> registerCustomer(@RequestBody CustomerRequest request) {

        Customer customer = customers.registerCustomer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }
}