package com.donkan.sisvetclinic.modules.finance.service.implementation;

import com.donkan.sisvetclinic.modules.finance.domain.Expense;
import com.donkan.sisvetclinic.modules.finance.service.dto.ExpenseRequest;
import com.donkan.sisvetclinic.modules.finance.service.dto.ReportFilter;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ExpenseUseCase;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ExpenseRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService implements ExpenseUseCase {

    private static final Set<String> CATEGORIES = Set.of(
            "UTILITY", "EQUIPMENT", "INFRASTRUCTURE", "SUPPLIES",
            "MARKETING", "SERVICES", "SECURITY", "OTHER"
    );

    private final ExpenseRepositoryPort expenses;

    public ExpenseService(ExpenseRepositoryPort expenses) {
        this.expenses = expenses;
    }

    @Override
    @Transactional
    public Expense registerExpense(Long userId, ExpenseRequest request) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El usuario responsable es inválido.");
        }

        if (request == null) {
            throw new IllegalArgumentException("Los datos del gasto son obligatorios.");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("El nombre del gasto es obligatorio.");
        }

        String name = request.name().trim();

        if (name.length() > 100) {
            throw new IllegalArgumentException("El nombre supera los 100 caracteres.");
        }

        BigDecimal amount = request.amount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero.");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe admite máximo dos decimales.");
        }

        amount = amount.setScale(2, RoundingMode.UNNECESSARY);

        if (amount.precision() > 12) {
            throw new IllegalArgumentException("El importe supera el límite permitido.");
        }

        String category = request.category();

        if (category == null || category.isBlank()) {
            category = "OTHER";
        }

        category = category.trim().toUpperCase(Locale.ROOT);

        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("La categoría del gasto no es válida.");
        }

        String description = request.description();

        if (description != null) {
            description = description.trim();
        }

        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("La descripción supera los 255 caracteres.");
        }

        LocalDateTime expenseDatetime = request.expenseDatetime();

        if (expenseDatetime == null) {
            expenseDatetime = LocalDateTime.now(ZoneId.of("America/Lima"));
        }

        Expense expense = new Expense();

        expense.setUserId(userId);
        expense.setName(name);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setExpenseDatetime(expenseDatetime);
        expense.setAutomatic(false);
        expense.setRecordStatus("Active");

        return expenses.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> listByPeriod(LocalDate startDate, LocalDate endDate) {

        new ReportFilter(startDate, endDate);

        return expenses.findByPeriod(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalByPeriod(LocalDate startDate, LocalDate endDate) {

        new ReportFilter(startDate, endDate);

        return expenses.getTotalByPeriod(startDate, endDate);
    }
}