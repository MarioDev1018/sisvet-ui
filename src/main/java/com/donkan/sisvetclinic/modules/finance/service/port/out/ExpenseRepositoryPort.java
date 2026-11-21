package com.donkan.sisvetclinic.modules.finance.service.port.out;

import com.donkan.sisvetclinic.modules.finance.domain.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepositoryPort {

    Expense save(Expense expense);

    List<Expense> findByPeriod(LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalByPeriod(LocalDate startDate, LocalDate endDate);

    boolean insertMonthlyIfAbsent(Expense expense);
}