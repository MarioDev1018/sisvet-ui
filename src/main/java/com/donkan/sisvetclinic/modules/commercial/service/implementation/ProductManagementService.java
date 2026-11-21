package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.*;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.*;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.ProductManagementUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

public class ProductManagementService implements ProductManagementUseCase {

    private static final Set<String> UNITS = Set.of("UNIT", "G", "ML");

    private final ProductRepositoryPort products;
    private final CategoryRepositoryPort categories;
    private final ProductPresentationRepositoryPort presentations;

    public ProductManagementService(ProductRepositoryPort products,
                                    CategoryRepositoryPort categories,
                                    ProductPresentationRepositoryPort presentations) {
        this.products = products;
        this.categories = categories;
        this.presentations = presentations;
    }

    @Override
    @Transactional
    public Product registerProduct(ProductRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Los datos del producto son obligatorios.");
        }

        String code = CommercialValidation.requireText(request.code(), "El código");
        String name = CommercialValidation.requireText(request.name(), "El nombre");
        BigDecimal price = CommercialValidation.requireMoney(request.price());
        Long categoryId = CommercialValidation.requireId(request.categoryId(), "Categoría");

        String baseUnit = request.baseUnit() == null
                ? "UNIT"
                : request.baseUnit().trim().toUpperCase(Locale.ROOT);

        if (!UNITS.contains(baseUnit)) {
            throw new IllegalArgumentException("La unidad base no es válida.");
        }

        if (products.existsByCode(code)) {
            throw new IllegalStateException("El código ya está registrado.");
        }

        categories.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("La categoría no existe."));

        int quantityInBase = request.presentationQuantity() == null
                ? 1
                : request.presentationQuantity();

        if (quantityInBase <= 0) {
            throw new IllegalArgumentException("La equivalencia debe ser mayor que cero.");
        }

        if (!"UNIT".equals(baseUnit) && request.presentationQuantity() == null) {
            throw new IllegalArgumentException("Indica la equivalencia en gramos o mililitros.");
        }

        String presentationName = request.presentationName() == null
                || request.presentationName().isBlank()
                ? "Unidad"
                : request.presentationName().trim();

        boolean fractionable = Boolean.TRUE.equals(request.fractionable());

        if (fractionable && "UNIT".equals(baseUnit)) {
            throw new IllegalArgumentException("Los productos por unidad no se pueden fraccionar.");
        }

        Product product = new Product();

        product.setCode(code);
        product.setName(name);
        product.setPrice(price);
        product.setStock(0);
        product.setCategoryId(categoryId);
        product.setBaseUnit(baseUnit);
        product.setActive(true);

        Product saved = products.save(product);

        ProductPresentation presentation = new ProductPresentation(
                null,
                saved.getId(),
                presentationName,
                quantityInBase,
                price,
                true,
                fractionable,
                true
        );

        presentations.save(presentation);

        return saved;
    }

    @Override
    @Transactional
    public ProductPresentation registerPresentation(Long productId, ProductPresentationRequest request) {

        CommercialValidation.requireId(productId, "Producto");

        if (request == null) {
            throw new IllegalArgumentException("La presentación es obligatoria.");
        }

        Product product = findById(productId);

        String name = CommercialValidation.requireText(request.name(), "La presentación");

        if (name.length() > 100 || request.quantityInBase() <= 0) {
            throw new IllegalArgumentException("Nombre o equivalencia inválidos.");
        }

        if ("UNIT".equals(product.getBaseUnit()) && request.fractionable()) {
            throw new IllegalArgumentException("Un artículo por unidad no puede fraccionarse.");
        }

        BigDecimal price = CommercialValidation.requireMoney(request.salePrice());

        ProductPresentation presentation = new ProductPresentation(
                null,
                productId,
                name,
                request.quantityInBase(),
                price,
                false,
                request.fractionable(),
                true
        );

        return presentations.save(presentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPresentation> listPresentations(Long productId) {
        findById(productId);
        return presentations.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long productId) {

        CommercialValidation.requireId(productId, "Producto");

        return products.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public Product findByCode(String code) {

        return products.findByCode(CommercialValidation.requireText(code, "Código"))
                .filter(Product::isActive)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return products.findAll().stream().filter(Product::isActive).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categories.findAll();
    }
}