package com.donkan.sisvetclinic.modules.finance.service.implementation;

import com.donkan.sisvetclinic.modules.finance.domain.Expense;
import com.donkan.sisvetclinic.modules.finance.service.port.in.MonthlyExpenseUseCase;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ExpenseRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyExpenseService implements MonthlyExpenseUseCase {

    private final ExpenseRepositoryPort expenses;
    private final long systemUserId;
    private final BigDecimal electricityCost;
    private final BigDecimal waterCost;
    private final ZoneId zone;

    public MonthlyExpenseService(
            ExpenseRepositoryPort expenses,
            @Value("${sisvet.finance.monthly.user-id:0}") long systemUserId,
            @Value("${sisvet.finance.monthly.electricity:300.00}") BigDecimal electricityCost,
            @Value("${sisvet.finance.monthly.water:80.00}") BigDecimal waterCost,
            @Value("${sisvet.finance.monthly.zone:America/Lima}") String zone) {

        this.expenses = expenses;
        this.systemUserId = systemUserId;
        this.electricityCost = electricityCost;
        this.waterCost = waterCost;
        this.zone = ZoneId.of(zone);
    }

    @Override
    public void generateMonthlyExpenses() {
        generateMonthlyExpenses(YearMonth.now(zone));
    }

    @Override
    @Transactional
    public int generateMonthlyExpenses(YearMonth period) {

        if (period == null) {
            throw new IllegalArgumentException("El período es obligatorio.");
        }

        if (systemUserId <= 0) {
            throw new IllegalStateException("Configura sisvet.finance.monthly.user-id.");
        }

        int created = 0;

        if (createMonthlyExpense(
                period, "LUZ", "LUZ",
                "Pago mensual de energía eléctrica", electricityCost)) {
            created++;
        }

        if (createMonthlyExpense(
                period, "AGUA", "AGUA",
                "Pago mensual del servicio de agua", waterCost)) {
            created++;
        }

        return created;
    }

    private boolean createMonthlyExpense(YearMonth period, String code,
                                         String name, String description,
                                         BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException("La configuración del gasto mensual es inválida.");
        }

        BigDecimal value = amount.setScale(2, RoundingMode.UNNECESSARY);

        if (value.precision() > 12) {
            throw new IllegalStateException("El importe recurrente supera el límite permitido.");
        }

        Expense expense = new Expense();

        expense.setUserId(systemUserId);
        expense.setName(name);
        expense.setDescription(description);
        expense.setAmount(value);
        expense.setCategory("UTILITY");
        expense.setExpenseDatetime(period.atDay(1).atStartOfDay());
        expense.setAutomatic(true);
        expense.setRecurringCode(code);
        expense.setBillingPeriod(period.atDay(1));
        expense.setRecordStatus("Active");

        return expenses.insertMonthlyIfAbsent(expense);
    }
}