package com.donkan.sisvetclinic.modules.finance.service.port.in;

import java.time.YearMonth;

public interface MonthlyExpenseUseCase {

    void generateMonthlyExpenses();

    int generateMonthlyExpenses(YearMonth period);
}