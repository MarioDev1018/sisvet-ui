package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.Sale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Formats plain-text confirmation receipts for commercial sales and clinical payments.
 */
final class CommercialReceiptFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CommercialReceiptFormatter() {}

    static String formatSaleReceipt(Sale sale, String paymentMethodName, List<String> itemLines) {
        return buildReceipt(
                "Boleta / Confirmación de compra #" + sale.getId(),
                sale,
                "Método de pago: " + safeText(paymentMethodName, "Registrado"),
                itemLines
        );
    }

    static String formatClinicalReceipt(Sale sale, Long consultationId, Long attentionId, List<String> chargeLines) {
        String reference = consultationId != null
                ? "Consulta clínica #" + consultationId
                : "Atención clínica #" + attentionId;
        return buildReceipt(
                "Comprobante de cobro clínico #" + sale.getId(),
                sale,
                "Referencia: " + reference,
                chargeLines
        );
    }

    private static String buildReceipt(String title, Sale sale, String extraHeader, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        if (sale.getSaleDatetime() != null) {
            sb.append("Fecha y hora: ").append(sale.getSaleDatetime().format(DATE_TIME)).append("\n");
        }
        sb.append(extraHeader).append("\n");
        if (lines != null && !lines.isEmpty()) {
            sb.append("\nDetalle:\n");
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        }
        sb.append("\nTotal pagado: S/ ").append(formatMoney(sale.getTotalAmount())).append("\n");
        sb.append("Referencia de operación: ").append(safeText(sale.getOperationKey(), String.valueOf(sale.getId())));
        return sb.toString();
    }

    static String formatMoney(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
