package com.donkan.sisvetclinic.modules.commercial.service.port.out;

import com.donkan.sisvetclinic.modules.commercial.domain.ProductPresentation;

import java.util.List;
import java.util.Optional;

public interface ProductPresentationRepositoryPort {

    ProductPresentation save(ProductPresentation presentation);

    Optional<ProductPresentation> findById(Long presentationId);

    Optional<ProductPresentation> findDefaultByProductId(Long productId);

    List<ProductPresentation> findByProductId(Long productId);
}