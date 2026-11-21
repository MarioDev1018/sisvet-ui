package com.donkan.sisvetclinic.modules.finance.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.finance.domain.Expense;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ExpenseRepositoryPort;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlExpenseAdapter implements ExpenseRepositoryPort {

    private final JdbcTemplate jdbc;

    public SqlExpenseAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Expense save(Expense expense) {

        String sql = """
            INSERT INTO [dbo].[Expense] (
                user_id,
                name,
                description,
                amount,
                category,
                expense_datetime,
                is_automatic,
                recurring_code,
                billing_period,
                record_status
            )
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Long id = jdbc.queryForObject(
                sql,
                Long.class,
                expense.getUserId(),
                expense.getName(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory(),
                Timestamp.valueOf(expense.getExpenseDatetime()),
                expense.isAutomatic(),
                expense.getRecurringCode(),
                expense.getBillingPeriod() == null ? null : Date.valueOf(expense.getBillingPeriod()),
                expense.getRecordStatus()
        );

        expense.setId(id);

        return expense;
    }

    @Override
    public List<Expense> findByPeriod(LocalDate startDate, LocalDate endDate) {

        String sql = """
            SELECT
                id,
                user_id,
                name,
                description,
                amount,
                category,
                expense_datetime,
                is_automatic,
                recurring_code,
                billing_period,
                record_status
            FROM [dbo].[Expense]
            WHERE record_status = 'Active'
              AND expense_datetime >= ?
              AND expense_datetime < ?
            ORDER BY expense_datetime DESC, id DESC
            """;

        return jdbc.query(
                sql,
                (rs, row) -> mapExpense(rs),
                Timestamp.valueOf(startDate.atStartOfDay()),
                Timestamp.valueOf(endDate.plusDays(1).atStartOfDay())
        );
    }

    @Override
    public BigDecimal getTotalByPeriod(LocalDate startDate, LocalDate endDate) {

        String sql = """
            SELECT COALESCE(SUM(amount), 0)
            FROM [dbo].[Expense]
            WHERE record_status = 'Active'
              AND expense_datetime >= ?
              AND expense_datetime < ?
            """;

        BigDecimal total = jdbc.queryForObject(
                sql,
                BigDecimal.class,
                Timestamp.valueOf(startDate.atStartOfDay()),
                Timestamp.valueOf(endDate.plusDays(1).atStartOfDay())
        );

        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public boolean insertMonthlyIfAbsent(Expense expense) {

        if (!expense.isAutomatic()
                || expense.getRecurringCode() == null
                || expense.getBillingPeriod() == null) {
            throw new IllegalArgumentException("La obligación recurrente está incompleta.");
        }

        String sql = """
            INSERT INTO [dbo].[Expense] (
                user_id,
                name,
                description,
                amount,
                category,
                expense_datetime,
                is_automatic,
                recurring_code,
                billing_period,
                record_status
            )
            SELECT ?, ?, ?, ?, ?, ?, 1, ?, ?, 'Active'
            WHERE NOT EXISTS (
                SELECT 1
                FROM [dbo].[Expense] WITH (UPDLOCK, HOLDLOCK)
                WHERE recurring_code = ?
                  AND billing_period = ?
                  AND is_automatic = 1
            )
            """;

        Date period = Date.valueOf(expense.getBillingPeriod());

        int rows = jdbc.update(
                sql,
                expense.getUserId(),
                expense.getName(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory(),
                Timestamp.valueOf(expense.getExpenseDatetime()),
                expense.getRecurringCode(),
                period,
                expense.getRecurringCode(),
                period
        );

        return rows == 1;
    }

    private Expense mapExpense(ResultSet rs) throws SQLException {

        Expense expense = new Expense();

        expense.setId(rs.getLong("id"));
        expense.setUserId(rs.getLong("user_id"));
        expense.setName(rs.getString("name"));
        expense.setDescription(rs.getString("description"));
        expense.setAmount(rs.getBigDecimal("amount"));
        expense.setCategory(rs.getString("category"));
        expense.setExpenseDatetime(rs.getTimestamp("expense_datetime").toLocalDateTime());
        expense.setAutomatic(rs.getBoolean("is_automatic"));
        expense.setRecurringCode(rs.getString("recurring_code"));

        Date billingPeriod = rs.getDate("billing_period");

        if (billingPeriod != null) {
            expense.setBillingPeriod(billingPeriod.toLocalDate());
        }

        expense.setRecordStatus(rs.getString("record_status"));

        return expense;
    }
}