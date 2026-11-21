package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findById(Long productId);

    Optional<Product> findByCode(String code);

    List<Product> findAll();

    boolean existsByCode(String code);
}