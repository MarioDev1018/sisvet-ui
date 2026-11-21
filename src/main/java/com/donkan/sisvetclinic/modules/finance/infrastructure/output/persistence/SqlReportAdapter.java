package com.donkan.sisvetclinic.modules.finance.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.finance.service.dto.*;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ReportQueryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlReportAdapter implements ReportQueryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public SqlReportAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource period(ReportFilter filter) {

        return new MapSqlParameterSource()
                .addValue("fromDate", Timestamp.valueOf(filter.startDate().atStartOfDay()))
                .addValue("toDate", Timestamp.valueOf(filter.endDate().plusDays(1).atStartOfDay()));
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    @Override
    public FinancialReport getFinancialSummary(ReportFilter filter) {

        String sql = """
            SELECT
                (
                    SELECT COUNT_BIG(*)
                    FROM [dbo].[Sale]
                    WHERE record_status = 'Active'
                      AND sale_datetime >= :fromDate
                      AND sale_datetime < :toDate
                ) AS sale_count,

                (
                    SELECT COALESCE(SUM(total_amount), 0)
                    FROM [dbo].[Sale]
                    WHERE record_status = 'Active'
                      AND sale_datetime >= :fromDate
                      AND sale_datetime < :toDate
                ) AS sales_total,

                (
                    SELECT COUNT_BIG(*)
                    FROM [dbo].[Purchase]
                    WHERE record_status = 'Active'
                      AND purchase_datetime >= :fromDate
                      AND purchase_datetime < :toDate
                ) AS purchase_count,

                (
                    SELECT COALESCE(SUM(total_amount), 0)
                    FROM [dbo].[Purchase]
                    WHERE record_status = 'Active'
                      AND purchase_datetime >= :fromDate
                      AND purchase_datetime < :toDate
                ) AS purchases_total,

                (
                    SELECT COUNT_BIG(*)
                    FROM [dbo].[Expense]
                    WHERE record_status = 'Active'
                      AND expense_datetime >= :fromDate
                      AND expense_datetime < :toDate
                ) AS expense_count,

                (
                    SELECT COALESCE(SUM(amount), 0)
                    FROM [dbo].[Expense]
                    WHERE record_status = 'Active'
                      AND expense_datetime >= :fromDate
                      AND expense_datetime < :toDate
                ) AS expenses_total
            """;

        return jdbc.queryForObject(sql, period(filter), (rs, row) -> {

            long saleCount = rs.getLong("sale_count");
            long purchaseCount = rs.getLong("purchase_count");
            long expenseCount = rs.getLong("expense_count");

            BigDecimal salesTotal = money(rs.getBigDecimal("sales_total"));
            BigDecimal purchasesTotal = money(rs.getBigDecimal("purchases_total"));
            BigDecimal expensesTotal = money(rs.getBigDecimal("expenses_total"));

            BigDecimal totalOutflows = purchasesTotal.add(expensesTotal);
            BigDecimal balance = salesTotal.subtract(totalOutflows);

            BigDecimal averageTicket = BigDecimal.ZERO;
            BigDecimal balancePercentage = BigDecimal.ZERO;

            if (saleCount > 0) {
                averageTicket = salesTotal.divide(BigDecimal.valueOf(saleCount), 2, RoundingMode.HALF_UP);
            }

            if (salesTotal.compareTo(BigDecimal.ZERO) > 0) {
                balancePercentage = balance.multiply(BigDecimal.valueOf(100))
                        .divide(salesTotal, 2, RoundingMode.HALF_UP);
            }

            return new FinancialReport(
                    filter.startDate(),
                    filter.endDate(),
                    saleCount,
                    salesTotal,
                    purchaseCount,
                    purchasesTotal,
                    expenseCount,
                    expensesTotal,
                    totalOutflows,
                    balance,
                    averageTicket,
                    balancePercentage
            );
        });
    }

    @Override
    public List<SalesReportRow> getSalesReport(ReportFilter filter) {

        String sql = """
            SELECT
                s.id AS sale_id,
                s.sale_datetime,
                u.full_name AS seller,
                c.full_name AS customer,
                pm.name AS payment_method,
                s.total_amount
            FROM [dbo].[Sale] s
            INNER JOIN [dbo].[User] u ON u.id = s.user_id
            INNER JOIN [dbo].[Customer] c ON c.id = s.customer_id
            INNER JOIN [dbo].[PaymentMethod] pm ON pm.id = s.payment_method_id
            WHERE s.record_status = 'Active'
              AND s.sale_datetime >= :fromDate
              AND s.sale_datetime < :toDate
            ORDER BY s.sale_datetime, s.id
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new SalesReportRow(
                rs.getLong("sale_id"),
                rs.getTimestamp("sale_datetime").toLocalDateTime(),
                rs.getString("seller"),
                rs.getString("customer"),
                rs.getString("payment_method"),
                rs.getBigDecimal("total_amount")
        ));
    }

    @Override
    public List<PurchaseReportRow> getPurchasesReport(ReportFilter filter) {

        String sql = """
            SELECT
                p.id AS purchase_id,
                p.purchase_datetime,
                u.full_name AS employee,
                s.business_name AS supplier,
                pm.name AS payment_method,
                p.total_amount
            FROM [dbo].[Purchase] p
            INNER JOIN [dbo].[User] u ON u.id = p.user_id
            INNER JOIN [dbo].[Supplier] s ON s.id = p.supplier_id
            INNER JOIN [dbo].[PaymentMethod] pm ON pm.id = p.payment_method_id
            WHERE p.record_status = 'Active'
              AND p.purchase_datetime >= :fromDate
              AND p.purchase_datetime < :toDate
            ORDER BY p.purchase_datetime, p.id
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new PurchaseReportRow(
                rs.getLong("purchase_id"),
                rs.getTimestamp("purchase_datetime").toLocalDateTime(),
                rs.getString("employee"),
                rs.getString("supplier"),
                rs.getString("payment_method"),
                rs.getBigDecimal("total_amount")
        ));
    }

    @Override
    public List<ExpenseReportRow> getExpensesReport(ReportFilter filter) {

        String sql = """
            SELECT
                e.id AS expense_id,
                e.expense_datetime,
                e.name,
                e.description,
                e.category,
                e.amount,
                e.is_automatic,
                u.full_name AS registered_by
            FROM [dbo].[Expense] e
            INNER JOIN [dbo].[User] u ON u.id = e.user_id
            WHERE e.record_status = 'Active'
              AND e.expense_datetime >= :fromDate
              AND e.expense_datetime < :toDate
            ORDER BY e.expense_datetime, e.id
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new ExpenseReportRow(
                rs.getLong("expense_id"),
                rs.getTimestamp("expense_datetime").toLocalDateTime(),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getBigDecimal("amount"),
                rs.getBoolean("is_automatic"),
                rs.getString("registered_by")
        ));
    }

    @Override
    public List<DailyMovementRow> getDailyMovement(ReportFilter filter) {

        String sql = """
            SELECT
                movement_date,
                SUM(sales) AS sales_total,
                SUM(purchases) AS purchases_total,
                SUM(expenses) AS expenses_total
            FROM (
                SELECT
                    CAST(sale_datetime AS DATE) AS movement_date,
                    total_amount AS sales,
                    CAST(0 AS DECIMAL(19,2)) AS purchases,
                    CAST(0 AS DECIMAL(19,2)) AS expenses
                FROM [dbo].[Sale]
                WHERE record_status = 'Active'
                  AND sale_datetime >= :fromDate
                  AND sale_datetime < :toDate

                UNION ALL

                SELECT
                    CAST(purchase_datetime AS DATE),
                    CAST(0 AS DECIMAL(19,2)),
                    total_amount,
                    CAST(0 AS DECIMAL(19,2))
                FROM [dbo].[Purchase]
                WHERE record_status = 'Active'
                  AND purchase_datetime >= :fromDate
                  AND purchase_datetime < :toDate

                UNION ALL

                SELECT
                    CAST(expense_datetime AS DATE),
                    CAST(0 AS DECIMAL(19,2)),
                    CAST(0 AS DECIMAL(19,2)),
                    amount
                FROM [dbo].[Expense]
                WHERE record_status = 'Active'
                  AND expense_datetime >= :fromDate
                  AND expense_datetime < :toDate
            ) movements
            GROUP BY movement_date
            ORDER BY movement_date
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> {

            BigDecimal sales = money(rs.getBigDecimal("sales_total"));
            BigDecimal purchases = money(rs.getBigDecimal("purchases_total"));
            BigDecimal expenses = money(rs.getBigDecimal("expenses_total"));

            return new DailyMovementRow(
                    rs.getDate("movement_date").toLocalDate(),
                    sales,
                    purchases,
                    expenses,
                    sales.subtract(purchases).subtract(expenses)
            );
        });
    }

    @Override
public List<ProductPerformanceRow> getProductPerformance(ReportFilter filter) {

    String sql = """
        WITH PurchaseData AS (
            SELECT
                pd.product_id,
                SUM(CAST(pd.base_quantity AS BIGINT)) AS purchased_quantity,
                SUM(pd.subtotal) AS purchase_total,
                CAST(
                    SUM(pd.subtotal) / NULLIF(SUM(pd.base_quantity), 0)
                    AS DECIMAL(18,6)
                ) AS average_cost
            FROM [dbo].[PurchaseDetail] pd
            INNER JOIN [dbo].[Purchase] pu ON pu.id = pd.purchase_id
            WHERE pu.record_status = 'Active'
              AND pu.purchase_datetime >= :fromDate
              AND pu.purchase_datetime < :toDate
            GROUP BY pd.product_id
        ),
        SaleData AS (
            SELECT
                sd.product_id,
                SUM(CAST(sd.base_quantity AS BIGINT)) AS sold_quantity,
                SUM(sd.subtotal) AS sales_amount,
                CAST(
                    SUM(sd.subtotal) / NULLIF(SUM(sd.base_quantity), 0)
                    AS DECIMAL(18,6)
                ) AS average_sale_price,
                SUM(sd.cost_total) AS known_cost,
                COUNT(*) AS detail_count,
                COUNT(sd.cost_total) AS valued_count
            FROM [dbo].[SaleDetail] sd
            INNER JOIN [dbo].[Sale] s ON s.id = sd.sale_id
            WHERE s.record_status = 'Active'
              AND s.sale_datetime >= :fromDate
              AND s.sale_datetime < :toDate
            GROUP BY sd.product_id
        )
        SELECT
            p.product_code,
            p.name AS product_name,
            c.name AS category,
            p.base_unit,
            COALESCE(pd.purchased_quantity, 0) AS purchased_quantity,
            pd.average_cost,
            COALESCE(pd.purchase_total, 0) AS purchase_total,
            COALESCE(sd.sold_quantity, 0) AS sold_quantity,
            sd.average_sale_price,
            COALESCE(sd.sales_amount, 0) AS sales_amount,
            CASE
                WHEN sd.product_id IS NULL THEN CAST(0 AS DECIMAL(18,4))
                WHEN sd.detail_count <> sd.valued_count THEN NULL
                ELSE sd.sales_amount - sd.known_cost
            END AS estimated_margin,
            p.stock_quantity
        FROM [dbo].[Product] p
        INNER JOIN [dbo].[Category] c ON c.id = p.category_id
        LEFT JOIN PurchaseData pd ON pd.product_id = p.id
        LEFT JOIN SaleData sd ON sd.product_id = p.id
        WHERE pd.product_id IS NOT NULL OR sd.product_id IS NOT NULL
        ORDER BY sales_amount DESC, purchased_quantity DESC
        """;

    return jdbc.query(sql, period(filter), (rs, row) -> new ProductPerformanceRow(
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getString("category"),
            rs.getString("base_unit"),
            rs.getLong("purchased_quantity"),
            rs.getBigDecimal("average_cost"),
            rs.getBigDecimal("purchase_total"),
            rs.getLong("sold_quantity"),
            rs.getBigDecimal("average_sale_price"),
            rs.getBigDecimal("sales_amount"),
            rs.getBigDecimal("estimated_margin"),
            rs.getInt("stock_quantity")
    ));
}

    @Override
public List<ProductSalesRow> getProductSales(ReportFilter filter) {

    String sql = """
        SELECT
            p.product_code,
            p.name AS product_name,
            c.name AS category,
            p.base_unit,
            SUM(CAST(sd.base_quantity AS BIGINT)) AS quantity_sold,
            CAST(
                SUM(sd.subtotal) / NULLIF(SUM(sd.base_quantity), 0)
                AS DECIMAL(18,6)
            ) AS average_price,
            SUM(sd.subtotal) AS total
        FROM [dbo].[SaleDetail] sd
        INNER JOIN [dbo].[Sale] s ON s.id = sd.sale_id
        INNER JOIN [dbo].[Product] p ON p.id = sd.product_id
        INNER JOIN [dbo].[Category] c ON c.id = p.category_id
        WHERE s.record_status = 'Active'
          AND s.sale_datetime >= :fromDate
          AND s.sale_datetime < :toDate
        GROUP BY p.id, p.product_code, p.name, c.name, p.base_unit
        ORDER BY quantity_sold DESC, total DESC
        """;

    return jdbc.query(sql, period(filter), (rs, row) -> new ProductSalesRow(
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getString("category"),
            rs.getString("base_unit"),
            rs.getLong("quantity_sold"),
            rs.getBigDecimal("average_price"),
            rs.getBigDecimal("total")
    ));
}

    @Override
    public List<CustomerFrequencyRow> getCustomerFrequency(ReportFilter filter) {

        String sql = """
            SELECT
                c.document_number,
                c.full_name AS customer_name,
                COUNT(s.id) AS purchase_count,
                SUM(s.total_amount) AS total_spent,
                CAST(AVG(s.total_amount) AS DECIMAL(19,2)) AS average_ticket
            FROM [dbo].[Sale] s
            INNER JOIN [dbo].[Customer] c ON c.id = s.customer_id
            WHERE s.record_status = 'Active'
              AND s.sale_datetime >= :fromDate
              AND s.sale_datetime < :toDate
              AND c.document_type <> 'S/D'
              AND c.document_number <> '00000000'
            GROUP BY c.id, c.document_number, c.full_name
            ORDER BY purchase_count DESC, total_spent DESC
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new CustomerFrequencyRow(
                rs.getString("document_number"),
                rs.getString("customer_name"),
                rs.getInt("purchase_count"),
                rs.getBigDecimal("total_spent"),
                rs.getBigDecimal("average_ticket")
        ));
    }

    @Override
    public List<SupplierPurchasesRow> getSupplierPurchases(ReportFilter filter) {

        String sql = """
            SELECT
                s.ruc,
                s.business_name AS supplier_name,
                COUNT(p.id) AS purchase_count,
                SUM(p.total_amount) AS total
            FROM [dbo].[Purchase] p
            INNER JOIN [dbo].[Supplier] s ON s.id = p.supplier_id
            WHERE p.record_status = 'Active'
              AND p.purchase_datetime >= :fromDate
              AND p.purchase_datetime < :toDate
            GROUP BY s.id, s.ruc, s.business_name
            ORDER BY total DESC, purchase_count DESC
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new SupplierPurchasesRow(
                rs.getString("ruc"),
                rs.getString("supplier_name"),
                rs.getInt("purchase_count"),
                rs.getBigDecimal("total")
        ));
    }

    @Override
    public List<SellerSalesRow> getSellerSales(ReportFilter filter) {

        String sql = """
            SELECT
                u.id AS user_id,
                u.full_name AS seller_name,
                COUNT(s.id) AS sale_count,
                SUM(s.total_amount) AS total,
                CAST(AVG(s.total_amount) AS DECIMAL(19,2)) AS average_ticket
            FROM [dbo].[Sale] s
            INNER JOIN [dbo].[User] u ON u.id = s.user_id
            WHERE s.record_status = 'Active'
              AND s.sale_datetime >= :fromDate
              AND s.sale_datetime < :toDate
            GROUP BY u.id, u.full_name
            ORDER BY total DESC, sale_count DESC
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new SellerSalesRow(
                rs.getLong("user_id"),
                rs.getString("seller_name"),
                rs.getInt("sale_count"),
                rs.getBigDecimal("total"),
                rs.getBigDecimal("average_ticket")
        ));
    }

    @Override
    public List<PaymentMethodSalesRow> getSalesByPaymentMethod(ReportFilter filter) {

        String sql = """
            SELECT
                pm.name AS payment_method,
                COUNT(s.id) AS sale_count,
                SUM(s.total_amount) AS total
            FROM [dbo].[Sale] s
            INNER JOIN [dbo].[PaymentMethod] pm ON pm.id = s.payment_method_id
            WHERE s.record_status = 'Active'
              AND s.sale_datetime >= :fromDate
              AND s.sale_datetime < :toDate
            GROUP BY pm.id, pm.name
            ORDER BY total DESC
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new PaymentMethodSalesRow(
                rs.getString("payment_method"),
                rs.getInt("sale_count"),
                rs.getBigDecimal("total")
        ));
    }

    @Override
    public List<ExpenseCategoryRow> getExpensesByCategory(ReportFilter filter) {

        String sql = """
            SELECT
                category,
                COUNT(id) AS expense_count,
                SUM(amount) AS total
            FROM [dbo].[Expense]
            WHERE record_status = 'Active'
              AND expense_datetime >= :fromDate
              AND expense_datetime < :toDate
            GROUP BY category
            ORDER BY total DESC
            """;

        return jdbc.query(sql, period(filter), (rs, row) -> new ExpenseCategoryRow(
                rs.getString("category"),
                rs.getInt("expense_count"),
                rs.getBigDecimal("total")
        ));
    }

    @Override
    public List<LowStockProductRow> getLowStockProducts(int threshold) {

        String sql = """
            SELECT
                p.id AS product_id,
                p.product_code,
                p.name AS product_name,
                c.name AS category,
                p.stock_quantity
            FROM [dbo].[Product] p
            INNER JOIN [dbo].[Category] c ON c.id = p.category_id
            WHERE p.record_status = 'Active'
              AND p.stock_quantity <= :threshold
            ORDER BY p.stock_quantity, p.name
            """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("threshold", threshold);

        return jdbc.query(sql, parameters, (rs, row) -> new LowStockProductRow(
                rs.getLong("product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getInt("stock_quantity")
        ));
    }
}