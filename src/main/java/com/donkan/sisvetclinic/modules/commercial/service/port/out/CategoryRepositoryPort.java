package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {

    Optional<Category> findById(Long categoryId);

    List<Category> findAll();
}