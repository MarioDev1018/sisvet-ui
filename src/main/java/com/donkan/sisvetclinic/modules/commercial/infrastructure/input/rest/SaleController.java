package com.donkan.sisvetclinic.modules.commercial.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.domain.SaleDetail;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.AuthResult;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.SaleUseCase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
public class SaleController {

    private final SaleUseCase sales;

    public SaleController(SaleUseCase sales) {
        this.sales = sales;
    }

    @PostMapping
    public ResponseEntity<SaleResult> registerSale(
            @AuthenticationPrincipal AuthResult user,
            @RequestBody SaleRequest request) {

        if (user == null) {
            throw new IllegalStateException("No existe un usuario autenticado.");
        }

        SaleResult result = sales.registerSale(user.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public List<Sale> listSales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null && endDate == null) {
            return sales.listSales();
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Debes proporcionar ambas fechas.");
        }

        return sales.listByPeriod(startDate, endDate);
    }

    @GetMapping("/{saleId}")
    public Sale findById(@PathVariable Long saleId) {
        return sales.findById(saleId);
    }

    @GetMapping("/{saleId}/details")
    public List<SaleDetail> findDetails(@PathVariable Long saleId) {
        return sales.findDetails(saleId);
    }
}