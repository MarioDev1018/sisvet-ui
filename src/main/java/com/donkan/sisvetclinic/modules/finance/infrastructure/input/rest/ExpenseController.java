package com.donkan.sisvetclinic.modules.finance.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.finance.domain.Expense;
import com.donkan.sisvetclinic.modules.finance.service.dto.ExpenseRequest;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ExpenseUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ExpenseController {

    private final ExpenseUseCase expenseUseCase;

    public ExpenseController(ExpenseUseCase expenseUseCase) {
        this.expenseUseCase = expenseUseCase;
    }

    @PostMapping
    public ResponseEntity<Expense> registerExpense(
            @AuthenticationPrincipal AuthResult user,
            @RequestBody ExpenseRequest request) {

        if (user == null) {
            throw new IllegalStateException("No existe una sesión autenticada.");
        }

        Expense expense = expenseUseCase.registerExpense(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping
    public List<Expense> listByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return expenseUseCase.listByPeriod(startDate, endDate);
    }

    @GetMapping("/total")
    public Map<String, BigDecimal> getTotalByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        BigDecimal total = expenseUseCase.getTotalByPeriod(startDate, endDate);

        return Map.of("total", total);
    }
}