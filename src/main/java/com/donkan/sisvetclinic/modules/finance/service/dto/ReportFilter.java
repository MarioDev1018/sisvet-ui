package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.time.LocalDate;

public record ReportFilter(
        LocalDate startDate,
        LocalDate endDate
) {

    public ReportFilter {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas del reporte son obligatorias.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
    }
}