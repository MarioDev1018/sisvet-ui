package com.donkan.sisvetclinic.modules.finance.service.port.out;

import com.donkan.sisvetclinic.modules.finance.service.dto.PdfReportDocument;

public interface ReportExportPort {

    byte[] generatePdf(PdfReportDocument report);
}