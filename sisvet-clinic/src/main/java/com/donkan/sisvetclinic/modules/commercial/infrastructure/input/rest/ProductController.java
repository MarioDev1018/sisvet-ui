package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.*;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.*;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.ProductManagementUseCase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductManagementUseCase products;

    public ProductController(ProductManagementUseCase products) {
        this.products = products;
    }

    @GetMapping
    public List<Product> listProducts() {
        return products.listProducts();
    }

    @GetMapping("/categories")
    public List<Category> listCategories() {
        return products.listCategories();
    }

    @GetMapping("/search")
    public Product findByCode(@RequestParam String code) {
        return products.findByCode(code);
    }

    @GetMapping("/{productId}")
    public Product findById(@PathVariable Long productId) {
        return products.findById(productId);
    }

    @GetMapping("/{productId}/presentations")
    public List<ProductPresentation> listPresentations(@PathVariable Long productId) {
        return products.listPresentations(productId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACEN')")
    public ResponseEntity<Product> registerProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(products.registerProduct(request));
    }

    @PostMapping("/{productId}/presentations")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACEN')")
    public ResponseEntity<ProductPresentation> registerPresentation(
            @PathVariable Long productId,
            @RequestBody ProductPresentationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(products.registerPresentation(productId, request));
    }
}