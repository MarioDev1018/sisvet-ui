package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.domain.Category;
import com.donkan.sisvetclinic.modules.commercial.domain.Product;
import com.donkan.sisvetclinic.modules.commercial.domain.ProductPresentation;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.ProductRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.ProductPresentationRequest;

import java.util.List;

public interface ProductManagementUseCase {

    Product registerProduct(ProductRequest request);

    Product findById(Long productId);

    Product findByCode(String code);

    List<Product> listProducts();

    List<Category> listCategories();

    ProductPresentation registerPresentation(Long productId, ProductPresentationRequest request);

    List<ProductPresentation> listPresentations(Long productId);
}