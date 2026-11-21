package com.donkan.sisvetclinic.modules.finance.service.port.in;

import com.donkan.sisvetclinic.modules.finance.service.dto.ReportFilter;

public interface ReportExportUseCase {

    byte[] exportPdf(String reportType, ReportFilter filter, int threshold);
}