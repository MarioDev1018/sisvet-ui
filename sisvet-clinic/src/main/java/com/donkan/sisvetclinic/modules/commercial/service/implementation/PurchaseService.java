package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.*;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.PurchaseRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.PurchaseResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.PurchaseUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

public class PurchaseService implements PurchaseUseCase {

    private final PurchaseRepositoryPort purchases;
    private final ProductRepositoryPort products;
    private final ProductPresentationRepositoryPort presentations;
    private final SupplierRepositoryPort suppliers;
    private final PaymentMethodRepositoryPort paymentMethods;
    private final InventoryPort inventory;

    public PurchaseService(PurchaseRepositoryPort purchases,
                           ProductRepositoryPort products,
                           ProductPresentationRepositoryPort presentations,
                           SupplierRepositoryPort suppliers,
                           PaymentMethodRepositoryPort paymentMethods,
                           InventoryPort inventory) {

        this.purchases = purchases;
        this.products = products;
        this.presentations = presentations;
        this.suppliers = suppliers;
        this.paymentMethods = paymentMethods;
        this.inventory = inventory;
    }

    @Override
    @Transactional
    public PurchaseResult registerPurchase(Long userId, PurchaseRequest request) {

        CommercialValidation.requireId(userId, "Usuario");

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La compra debe contener productos.");
        }

        CommercialValidation.requireId(request.supplierId(), "Proveedor");
        CommercialValidation.requireId(request.paymentMethodId(), "Método de pago");

        String key = CommercialValidation.requireOperationKey(request.operationKey());

        List<PurchaseDetail> details = new ArrayList<>();
        List<String> fingerprintItems = new ArrayList<>();
        Set<Long> usedPresentations = new HashSet<>();

        for (PurchaseRequest.Item item : request.items()) {

            if (item == null) {
                throw new IllegalArgumentException("Detalle de compra inválido.");
            }

            Long productId = CommercialValidation.requireId(item.productId(), "Producto");
            int quantity = CommercialValidation.requireQuantity(item.quantity());
            BigDecimal cost = CommercialValidation.requireMoney(item.unitCost());

            Product product = products.findById(productId)
                    .filter(Product::isActive)
                    .orElseThrow(() -> new NoSuchElementException("Producto no disponible."));

            ProductPresentation presentation = resolvePresentation(
                    productId, item.presentationId()
            );

            if (!presentation.isActive()) {
                throw new IllegalStateException("La presentación está inactiva.");
            }

            if (!usedPresentations.add(presentation.getId())) {
                throw new IllegalArgumentException("La presentación está repetida.");
            }

            int baseQuantity = Math.multiplyExact(quantity, presentation.getQuantityInBase());

            details.add(new PurchaseDetail(
                    null,
                    null,
                    product.getId(),
                    presentation.getId(),
                    quantity,
                    baseQuantity,
                    cost
            ));

            fingerprintItems.add(
                    productId + ":" + presentation.getId() + ":" + quantity + ":" + cost
            );
        }

        fingerprintItems.sort(String::compareTo);

        String fingerprint = CommercialValidation.fingerprint(
                "PURCHASE|" + userId + "|" + request.supplierId() + "|"
                + request.paymentMethodId() + "|" + String.join(";", fingerprintItems)
        );

        Purchase previous = purchases.findByOperationKey(key).orElse(null);

        if (previous != null) {

            if (!fingerprint.equals(previous.getRequestFingerprint())) {
                throw new IllegalStateException("La clave de operación fue utilizada con otros datos.");
            }

            return buildResult(previous);
        }

        Supplier supplier = suppliers.findById(request.supplierId())
                .filter(Supplier::isActive)
                .orElseThrow(() -> new NoSuchElementException("Proveedor no disponible."));

        PaymentMethod paymentMethod = paymentMethods.findById(request.paymentMethodId())
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new NoSuchElementException("Método de pago no disponible."));

        Purchase purchase = new Purchase();

        purchase.setUserId(userId);
        purchase.setSupplierId(supplier.getId());
        purchase.setPaymentMethodId(paymentMethod.getId());
        purchase.setPurchaseDatetime(LocalDateTime.now(ZoneId.of("America/Lima")));
        purchase.setRecordStatus("Active");
        purchase.setOperationKey(key);
        purchase.setRequestFingerprint(fingerprint);
        purchase.setDetails(details);

        purchase.calculateTotal();

        Purchase saved = purchases.save(purchase);

        for (PurchaseDetail detail : details) {
            detail.setPurchaseId(saved.getId());
        }

        purchases.saveDetails(saved.getId(), details);

        for (PurchaseDetail detail : details) {
            inventory.increaseStock(
                    detail.getProductId(),
                    detail.getBaseQuantity(),
                    detail.getSubtotal()
            );
        }

        return buildResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> listPaymentMethods() {
        return paymentMethods.findAllActive();
    }

    private ProductPresentation resolvePresentation(Long productId, Long presentationId) {

        ProductPresentation presentation = presentationId == null
                ? presentations.findDefaultByProductId(productId).orElseThrow()
                : presentations.findById(presentationId).orElseThrow();

        if (!productId.equals(presentation.getProductId())) {
            throw new IllegalArgumentException("La presentación no pertenece al producto.");
        }

        return presentation;
    }

    private PurchaseResult buildResult(Purchase purchase) {

        return new PurchaseResult(
                purchase.getId(),
                purchase.getTotalAmount(),
                purchase.getPurchaseDatetime(),
                purchase.getRecordStatus()
        );
    }
}