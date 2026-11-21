package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.util.List;

public record PdfReportDocument(
        String title,
        String subtitle,
        List<PdfReportSection> sections
) {
}