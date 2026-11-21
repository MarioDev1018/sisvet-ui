package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.*;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SaleUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

public class SaleService implements SaleUseCase {

    private final SaleRepositoryPort sales;
    private final ProductRepositoryPort products;
    private final ProductPresentationRepositoryPort presentations;
    private final CustomerRepositoryPort customers;
    private final PaymentMethodRepositoryPort paymentMethods;
    private final InventoryPort inventory;

    public SaleService(SaleRepositoryPort sales,
                       ProductRepositoryPort products,
                       ProductPresentationRepositoryPort presentations,
                       CustomerRepositoryPort customers,
                       PaymentMethodRepositoryPort paymentMethods,
                       InventoryPort inventory) {

        this.sales = sales;
        this.products = products;
        this.presentations = presentations;
        this.customers = customers;
        this.paymentMethods = paymentMethods;
        this.inventory = inventory;
    }

    @Override
    @Transactional
    public SaleResult registerSale(Long userId, SaleRequest request) {

        CommercialValidation.requireId(userId, "Usuario");

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener productos.");
        }

        CommercialValidation.requireId(request.customerId(), "Cliente");
        CommercialValidation.requireId(request.paymentMethodId(), "Método de pago");

        String key = CommercialValidation.requireOperationKey(request.operationKey());

        List<SaleDetail> details = new ArrayList<>();
        List<String> fingerprintItems = new ArrayList<>();
        Set<Long> usedPresentations = new HashSet<>();

        for (SaleRequest.Item item : request.items()) {

            if (item == null) {
                throw new IllegalArgumentException("Detalle de venta inválido.");
            }

            Long productId = CommercialValidation.requireId(item.productId(), "Producto");
            int quantity = CommercialValidation.requireQuantity(item.quantity());

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

            int baseQuantity;
            int saleQuantity;
            BigDecimal unitPrice;

            if (item.requestedBaseQuantity() != null) {

                if (!presentation.isFractionable() || "UNIT".equals(product.getBaseUnit())) {
                    throw new IllegalArgumentException("La presentación no admite fraccionamiento.");
                }

                if (quantity != 1) {
                    throw new IllegalArgumentException("Una venta fraccionada utiliza un solo detalle.");
                }

                baseQuantity = CommercialValidation.requireQuantity(item.requestedBaseQuantity());
                saleQuantity = 1;

                unitPrice = presentation.getSalePrice()
                        .multiply(BigDecimal.valueOf(baseQuantity))
                        .divide(
                                BigDecimal.valueOf(presentation.getQuantityInBase()),
                                2,
                                RoundingMode.HALF_UP
                        );

                if (unitPrice.signum() <= 0) {
                    throw new IllegalArgumentException("El importe fraccionado es demasiado pequeño.");
                }

            } else {

                baseQuantity = Math.multiplyExact(quantity, presentation.getQuantityInBase());
                saleQuantity = quantity;
                unitPrice = CommercialValidation.requireMoney(presentation.getSalePrice());
            }

            details.add(new SaleDetail(
                    null,
                    null,
                    productId,
                    presentation.getId(),
                    saleQuantity,
                    baseQuantity,
                    unitPrice
            ));

            fingerprintItems.add(
                    productId + ":" + presentation.getId() + ":"
                    + quantity + ":" + baseQuantity + ":" + item.requestedBaseQuantity()
            );
        }

        fingerprintItems.sort(String::compareTo);

        String fingerprint = CommercialValidation.fingerprint(
                "SALE|" + userId + "|" + request.customerId() + "|"
                + request.paymentMethodId() + "|" + String.join(";", fingerprintItems)
        );

        Sale previous = sales.findByOperationKey(key).orElse(null);

        if (previous != null) {

            if (!fingerprint.equals(previous.getRequestFingerprint())) {
                throw new IllegalStateException("La clave de operación fue utilizada con otros datos.");
            }

            return buildResult(previous);
        }

        Customer customer = customers.findById(request.customerId())
                .filter(Customer::isActive)
                .orElseThrow(() -> new NoSuchElementException("Cliente no disponible."));

        PaymentMethod paymentMethod = paymentMethods.findById(request.paymentMethodId())
                .filter(PaymentMethod::isActive)
                .orElseThrow(() -> new NoSuchElementException("Método de pago no disponible."));

        Sale sale = new Sale();

        sale.setUserId(userId);
        sale.setCustomerId(customer.getId());
        sale.setPaymentMethodId(paymentMethod.getId());
        sale.setSaleDatetime(LocalDateTime.now(ZoneId.of("America/Lima")));
        sale.setRecordStatus("Active");
        sale.setOperationType("PRODUCT");
        sale.setOperationKey(key);
        sale.setRequestFingerprint(fingerprint);
        sale.setDetails(details);

        sale.calculateTotal();

        BigDecimal totalCost = BigDecimal.ZERO;
        boolean costKnown = true;

        for (SaleDetail detail : details) {

            InventoryDeduction deduction = inventory.deductStock(
                    detail.getProductId(),
                    detail.getBaseQuantity()
            );

            if (!deduction.successful()) {
                throw new IllegalStateException(
                        "Stock insuficiente para el producto " + detail.getProductId()
                );
            }

            if (deduction.averageUnitCost() == null) {
                costKnown = false;
                continue;
            }

            BigDecimal cost = deduction.averageUnitCost()
                    .multiply(BigDecimal.valueOf(detail.getBaseQuantity()))
                    .setScale(4, RoundingMode.HALF_UP);

            detail.setCostTotal(cost);
            totalCost = totalCost.add(cost);
        }

        if (costKnown) {
            sale.setCostTotal(totalCost);
            sale.setGrossMargin(sale.getTotalAmount().subtract(totalCost));
        }

        Sale saved = sales.save(sale);

        for (SaleDetail detail : details) {
            detail.setSaleId(saved.getId());
        }

        sales.saveDetails(saved.getId(), details);

        return buildResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Sale findById(Long saleId) {
        return sales.findById(CommercialValidation.requireId(saleId, "Venta"))
                .orElseThrow(() -> new NoSuchElementException("La venta no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleDetail> findDetails(Long saleId) {
        findById(saleId);
        return sales.findDetailsBySaleId(saleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> listSales() {
        return sales.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sale> listByPeriod(LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("El período no es válido.");
        }

        return sales.findByPeriod(startDate, endDate);
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

    private SaleResult buildResult(Sale sale) {

        return new SaleResult(
                sale.getId(),
                sale.getTotalAmount(),
                sale.getSaleDatetime(),
                sale.getRecordStatus()
        );
    }
}