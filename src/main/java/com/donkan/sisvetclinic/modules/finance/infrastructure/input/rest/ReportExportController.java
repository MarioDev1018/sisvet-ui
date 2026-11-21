package com.donkan.sisvetclinic.modules.finance.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.finance.service.dto.ReportFilter;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ReportExportUseCase;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReportExportController {

    private final ReportExportUseCase exporter;

    public ReportExportController(ReportExportUseCase exporter) {
        this.exporter = exporter;
    }

    @GetMapping(value = "/{type}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int threshold) {

        ReportFilter filter = null;

        if (!"low-stock".equals(type)) {
            filter = new ReportFilter(startDate, endDate);
        }

        byte[] pdf = exporter.exportPdf(type, filter, threshold);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"sisvet-" + type + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}