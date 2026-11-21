package com.donkan.sisvetclinic.modules.commercial.infrastructure.config;

import com.donkan.sisvetclinic.modules.commercial.service.implementation.*;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SaleUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommercialBeansConfig {

    @Bean
    public UserManagementService userManagementService(
            UserRepositoryPort users,
            RoleRepositoryPort roles) {

        return new UserManagementService(users, roles);
    }

    @Bean
    public ProductManagementService productManagementService(
            ProductRepositoryPort products,
            CategoryRepositoryPort categories,
            ProductPresentationRepositoryPort presentations) {

        return new ProductManagementService(products, categories, presentations);
    }

    @Bean
    public CustomerManagementService customerManagementService(
            CustomerRepositoryPort customers) {

        return new CustomerManagementService(customers);
    }

    @Bean
    public SupplierManagementService supplierManagementService(
            SupplierRepositoryPort suppliers) {

        return new SupplierManagementService(suppliers);
    }

    @Bean
    public PurchaseService purchaseService(
            PurchaseRepositoryPort purchases,
            ProductRepositoryPort products,
            ProductPresentationRepositoryPort presentations,
            SupplierRepositoryPort suppliers,
            PaymentMethodRepositoryPort paymentMethods,
            InventoryPort inventory) {

        return new PurchaseService(
                purchases, products, presentations, suppliers, paymentMethods, inventory
        );
    }

    @Bean
    public SaleService saleService(
            SaleRepositoryPort sales,
            ProductRepositoryPort products,
            ProductPresentationRepositoryPort presentations,
            CustomerRepositoryPort customers,
            PaymentMethodRepositoryPort paymentMethods,
            InventoryPort inventory) {

        return new SaleService(
                sales, products, presentations, customers, paymentMethods, inventory
        );
    }

    @Bean
    public CheckoutFinalizationService checkoutFinalizationService(
            CheckoutRepositoryPort checkouts,
            SaleUseCase sales) {

        return new CheckoutFinalizationService(checkouts, sales);
    }

    @Bean
    public CheckoutService checkoutService(
            CheckoutRepositoryPort checkouts,
            SaleUseCase sales,
            PaymentProcessingPort payments,
            CheckoutFinalizationService finalization) {

        return new CheckoutService(checkouts, sales, payments, finalization);
    }

    @Bean
    public SalePaymentService salePaymentService(
            SaleRepositoryPort sales,
            PaymentProcessingPort payments) {

        return new SalePaymentService(sales, payments);
    }

    @Bean
    public ClinicalBillingService clinicalBillingService(
            ClinicalChargePort charges,
            SaleRepositoryPort sales,
            CustomerRepositoryPort customers,
            PaymentMethodRepositoryPort paymentMethods) {

        return new ClinicalBillingService(charges, sales, customers, paymentMethods);
    }

    @Bean
    public InvoiceService invoiceService(
            InvoiceRepositoryPort invoices,
            SaleRepositoryPort sales,
            InvoiceGateway gateway) {

        return new InvoiceService(invoices, sales, gateway);
    }
}