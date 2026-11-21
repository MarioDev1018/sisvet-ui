package com.donkan.sisvetclinic.modules.finance.infrastructure.input.scheduler;

import com.donkan.sisvetclinic.modules.finance.service.port.in.MonthlyExpenseUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "sisvet.finance.monthly.enabled",
        havingValue = "true"
)
public class MonthlyExpenseScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyExpenseScheduler.class);

    private final MonthlyExpenseUseCase monthlyExpenseUseCase;

    public MonthlyExpenseScheduler(MonthlyExpenseUseCase monthlyExpenseUseCase) {
        this.monthlyExpenseUseCase = monthlyExpenseUseCase;
    }

    @Scheduled(
            cron = "${sisvet.finance.monthly.cron:0 10 1 * * *}",
            zone = "${sisvet.finance.monthly.zone:America/Lima}"
    )
    public void runMonthlyExpenses() {

        try {

            monthlyExpenseUseCase.generateMonthlyExpenses();

            logger.info("Verificación de gastos mensuales completada.");

        } catch (Exception exception) {

            logger.error("Error al generar gastos mensuales.", exception);
        }
    }
}