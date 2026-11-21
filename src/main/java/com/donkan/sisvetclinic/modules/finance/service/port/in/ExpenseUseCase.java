package com.donkan.sisvetclinic.modules.finance.service.port.in;

import com.donkan.sisvetclinic.modules.finance.domain.Expense;
import com.donkan.sisvetclinic.modules.finance.service.dto.ExpenseRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseUseCase {

    Expense registerExpense(Long userId, ExpenseRequest request);

    List<Expense> listByPeriod(LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalByPeriod(LocalDate startDate, LocalDate endDate);
}