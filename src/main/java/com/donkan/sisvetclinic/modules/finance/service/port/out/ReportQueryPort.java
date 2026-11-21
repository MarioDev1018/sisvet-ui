package com.donkan.sisvetclinic.modules.finance.service.port.out;

import com.donkan.sisvetclinic.modules.finance.service.dto.*;

import java.util.List;

public interface ReportQueryPort {

    FinancialReport getFinancialSummary(ReportFilter filter);

    List<SalesReportRow> getSalesReport(ReportFilter filter);

    List<PurchaseReportRow> getPurchasesReport(ReportFilter filter);

    List<ExpenseReportRow> getExpensesReport(ReportFilter filter);

    List<DailyMovementRow> getDailyMovement(ReportFilter filter);

    List<ProductPerformanceRow> getProductPerformance(ReportFilter filter);

    List<ProductSalesRow> getProductSales(ReportFilter filter);

    List<CustomerFrequencyRow> getCustomerFrequency(ReportFilter filter);

    List<SupplierPurchasesRow> getSupplierPurchases(ReportFilter filter);

    List<SellerSalesRow> getSellerSales(ReportFilter filter);

    List<PaymentMethodSalesRow> getSalesByPaymentMethod(ReportFilter filter);

    List<ExpenseCategoryRow> getExpensesByCategory(ReportFilter filter);

    List<LowStockProductRow> getLowStockProducts(int threshold);
}