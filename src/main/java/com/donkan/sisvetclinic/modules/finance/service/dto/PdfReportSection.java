package com.donkan.sisvetclinic.modules.finance.service.dto;

import java.util.List;

public record PdfReportSection(
        String heading,
        List<String> lines
) {
}