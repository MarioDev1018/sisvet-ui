package com.donkan.sisvetclinic.modules.finance.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.finance.service.dto.*;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ReportUseCase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportUseCase reportUseCase;

    public ReportController(ReportUseCase reportUseCase) {
        this.reportUseCase = reportUseCase;
    }

    private ReportFilter filter(LocalDate startDate, LocalDate endDate) {
        return new ReportFilter(startDate, endDate);
    }

    @GetMapping("/financial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public FinancialReport getFinancialSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getFinancialSummary(filter(startDate, endDate));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<SalesReportRow> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getSalesReport(filter(startDate, endDate));
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PurchaseReportRow> getPurchasesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getPurchasesReport(filter(startDate, endDate));
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ExpenseReportRow> getExpensesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getExpensesReport(filter(startDate, endDate));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<DailyMovementRow> getDailyMovement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getDailyMovement(filter(startDate, endDate));
    }

    @GetMapping("/product-performance")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ProductPerformanceRow> getProductPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getProductPerformance(filter(startDate, endDate));
    }

    @GetMapping("/product-sales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ProductSalesRow> getProductSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getProductSales(filter(startDate, endDate));
    }

    @GetMapping("/customer-frequency")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<CustomerFrequencyRow> getCustomerFrequency(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getCustomerFrequency(filter(startDate, endDate));
    }

    @GetMapping("/supplier-purchases")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<SupplierPurchasesRow> getSupplierPurchases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getSupplierPurchases(filter(startDate, endDate));
    }

    @GetMapping("/seller-sales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<SellerSalesRow> getSellerSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getSellerSales(filter(startDate, endDate));
    }

    @GetMapping("/payment-method-sales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PaymentMethodSalesRow> getSalesByPaymentMethod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getSalesByPaymentMethod(filter(startDate, endDate));
    }

    @GetMapping("/expense-categories")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ExpenseCategoryRow> getExpensesByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return reportUseCase.getExpensesByCategory(filter(startDate, endDate));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACEN')")
    public List<LowStockProductRow> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {

        return reportUseCase.getLowStockProducts(threshold);
    }
}