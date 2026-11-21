package com.donkan.sisvetclinic.modules.finance.service.implementation;

import com.donkan.sisvetclinic.modules.finance.service.dto.*;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ReportUseCase;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ReportQueryPort;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService implements ReportUseCase {

    private final ReportQueryPort queries;

    public ReportService(ReportQueryPort queries) {
        this.queries = queries;
    }

    @Override
    public FinancialReport getFinancialSummary(ReportFilter filter) {
        return queries.getFinancialSummary(Objects.requireNonNull(filter));
    }

    @Override
    public List<SalesReportRow> getSalesReport(ReportFilter filter) {
        return queries.getSalesReport(Objects.requireNonNull(filter));
    }

    @Override
    public List<PurchaseReportRow> getPurchasesReport(ReportFilter filter) {
        return queries.getPurchasesReport(Objects.requireNonNull(filter));
    }

    @Override
    public List<ExpenseReportRow> getExpensesReport(ReportFilter filter) {
        return queries.getExpensesReport(Objects.requireNonNull(filter));
    }

    @Override
    public List<DailyMovementRow> getDailyMovement(ReportFilter filter) {
        return queries.getDailyMovement(Objects.requireNonNull(filter));
    }

    @Override
    public List<ProductPerformanceRow> getProductPerformance(ReportFilter filter) {
        return queries.getProductPerformance(Objects.requireNonNull(filter));
    }

    @Override
    public List<ProductSalesRow> getProductSales(ReportFilter filter) {
        return queries.getProductSales(Objects.requireNonNull(filter));
    }

    @Override
    public List<CustomerFrequencyRow> getCustomerFrequency(ReportFilter filter) {
        return queries.getCustomerFrequency(Objects.requireNonNull(filter));
    }

    @Override
    public List<SupplierPurchasesRow> getSupplierPurchases(ReportFilter filter) {
        return queries.getSupplierPurchases(Objects.requireNonNull(filter));
    }

    @Override
    public List<SellerSalesRow> getSellerSales(ReportFilter filter) {
        return queries.getSellerSales(Objects.requireNonNull(filter));
    }

    @Override
    public List<PaymentMethodSalesRow> getSalesByPaymentMethod(ReportFilter filter) {
        return queries.getSalesByPaymentMethod(Objects.requireNonNull(filter));
    }

    @Override
    public List<ExpenseCategoryRow> getExpensesByCategory(ReportFilter filter) {
        return queries.getExpensesByCategory(Objects.requireNonNull(filter));
    }

    @Override
    public List<LowStockProductRow> getLowStockProducts(int threshold) {

        if (threshold < 0) {
            throw new IllegalArgumentException("El umbral no puede ser negativo.");
        }

        return queries.getLowStockProducts(threshold);
    }
}