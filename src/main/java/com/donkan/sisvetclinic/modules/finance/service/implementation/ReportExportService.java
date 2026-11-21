package com.donkan.sisvetclinic.modules.finance.service.implementation;

import com.donkan.sisvetclinic.modules.finance.service.dto.*;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ReportExportUseCase;
import com.donkan.sisvetclinic.modules.finance.service.port.in.ReportUseCase;
import com.donkan.sisvetclinic.modules.finance.service.port.out.ReportExportPort;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ReportExportService implements ReportExportUseCase {

    private final ReportUseCase reports;
    private final ReportExportPort exporter;

    private static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("financial", "Resumen financiero"),
            Map.entry("sales", "Ventas"),
            Map.entry("purchases", "Compras"),
            Map.entry("expenses", "Gastos"),
            Map.entry("daily", "Movimientos diarios"),
            Map.entry("product-performance", "Rendimiento de productos"),
            Map.entry("product-sales", "Productos vendidos"),
            Map.entry("customer-frequency", "Frecuencia de clientes"),
            Map.entry("supplier-purchases", "Compras por proveedor"),
            Map.entry("seller-sales", "Ventas por vendedor"),
            Map.entry("payment-method-sales", "Ventas por método de pago"),
            Map.entry("expense-categories", "Gastos por categoría"),
            Map.entry("low-stock", "Productos con bajo stock")
    );

    public ReportExportService(ReportUseCase reports, ReportExportPort exporter) {
        this.reports = reports;
        this.exporter = exporter;
    }

    @Override
    public byte[] exportPdf(String reportType, ReportFilter filter, int threshold) {

        String title = TITLES.get(reportType);

        if (title == null) {
            throw new IllegalArgumentException("El reporte solicitado no existe.");
        }

        if (!"low-stock".equals(reportType) && filter == null) {
            throw new IllegalArgumentException("El período del reporte es obligatorio.");
        }

        Object data = switch (reportType) {
            case "financial" -> reports.getFinancialSummary(filter);
            case "sales" -> reports.getSalesReport(filter);
            case "purchases" -> reports.getPurchasesReport(filter);
            case "expenses" -> reports.getExpensesReport(filter);
            case "daily" -> reports.getDailyMovement(filter);
            case "product-performance" -> reports.getProductPerformance(filter);
            case "product-sales" -> reports.getProductSales(filter);
            case "customer-frequency" -> reports.getCustomerFrequency(filter);
            case "supplier-purchases" -> reports.getSupplierPurchases(filter);
            case "seller-sales" -> reports.getSellerSales(filter);
            case "payment-method-sales" -> reports.getSalesByPaymentMethod(filter);
            case "expense-categories" -> reports.getExpensesByCategory(filter);
            case "low-stock" -> reports.getLowStockProducts(threshold);
            default -> throw new IllegalArgumentException("Reporte no reconocido.");
        };

        String subtitle = filter == null
                ? "Umbral de stock: " + threshold
                : "Período: " + filter.startDate() + " al " + filter.endDate();

        List<PdfReportSection> sections = buildSections(data);

        PdfReportDocument document = new PdfReportDocument(title, subtitle, sections);

        return exporter.generatePdf(document);
    }

    private List<PdfReportSection> buildSections(Object data) {

        List<PdfReportSection> sections = new ArrayList<>();

        if (data instanceof List<?> records) {

            int index = 1;

            for (Object record : records) {

                List<String> lines = buildLines(record);

                sections.add(new PdfReportSection("Registro " + index, lines));

                index++;
            }

            if (sections.isEmpty()) {
                sections.add(new PdfReportSection(
                        "Resultados",
                        List.of("No se encontraron registros.")
                ));
            }

        } else if (data != null) {

            sections.add(new PdfReportSection(
                    "Indicadores",
                    buildLines(data)
            ));

        } else {

            sections.add(new PdfReportSection(
                    "Resultados",
                    List.of("No existen datos disponibles.")
            ));
        }

        return sections;
    }

    private List<String> buildLines(Object record) {

        List<String> lines = new ArrayList<>();

        if (record == null) {
            lines.add("No disponible");
            return lines;
        }

        Class<?> recordClass = record.getClass();

        if (!recordClass.isRecord()) {
            lines.add(String.valueOf(record));
            return lines;
        }

        RecordComponent[] components = recordClass.getRecordComponents();

        for (RecordComponent component : components) {

            try {

                String key = component.getName();

                Object value = component.getAccessor().invoke(record);

                String label = formatLabel(key);
                String formattedValue = formatValue(key, value);

                lines.add(label + ": " + formattedValue);

            } catch (ReflectiveOperationException exception) {

                throw new IllegalStateException(
                        "No se pudo obtener la información del reporte.",
                        exception
                );
            }
        }

        return lines;
    }

    private String formatLabel(String key) {

        String label = key.replaceAll("([a-z])([A-Z])", "$1 $2");

        if (label.isEmpty()) {
            return label;
        }

        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private String formatValue(String key, Object value) {

        if (value == null) {
            return "No disponible";
        }

        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "Sí" : "No";
        }

        if (!(value instanceof Number number)) {
            return String.valueOf(value);
        }

        String normalized = key.toLowerCase(Locale.ROOT);

        if (normalized.contains("percentage") || normalized.contains("percent")) {

            BigDecimal percentage = new BigDecimal(number.toString());

            return percentage.setScale(2, RoundingMode.HALF_UP).toPlainString() + " %";
        }

        if (isMonetaryField(normalized)) {

            BigDecimal amount = new BigDecimal(number.toString());

            int decimals = normalized.contains("averagecost")
                    || normalized.contains("averageprice")
                    || normalized.contains("averageunitcost")
                    || normalized.contains("averagesaleprice")
                    ? 6 : 2;

            return "S/ " + amount.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
        }

        return number.toString();
    }

    private boolean isMonetaryField(String key) {

        return key.contains("amount")
                || key.contains("total")
                || key.contains("price")
                || key.contains("cost")
                || key.contains("margin")
                || key.contains("balance")
                || key.contains("ticket")
                || key.contains("spent");
    }
}