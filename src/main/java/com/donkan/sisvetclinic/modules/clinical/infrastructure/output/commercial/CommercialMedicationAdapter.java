package com.donkan.sisvetclinic.modules.clinical.infrastructure.output.commercial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.donkan.sisvetclinic.modules.clinical.domain.MedicationDetail;
import com.donkan.sisvetclinic.modules.clinical.service.dto.response.VaccineProductOption;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.MedicationLookupPort;

@Repository
public class CommercialMedicationAdapter implements MedicationLookupPort {

    private final JdbcTemplate jdbc;

    public CommercialMedicationAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MedicationDetail> search(String text) {
        String term = "%" + (text == null ? "" : text.trim()) + "%";
        return jdbc.query(
            """
            SELECT p.id, p.name,
                   COALESCE(
                       NULLIF(LTRIM(RTRIM(d.active_ingredient)), ''),
                       'Stock: ' + CAST((p.stock_quantity - p.reserved_quantity) AS VARCHAR(30)) + ' ' + p.base_unit
                   ) AS active_ingredient,
                   COALESCE(NULLIF(LTRIM(RTRIM(d.concentration)), ''), pp.presentation, p.base_unit) AS concentration,
                   COALESCE(pp.presentation, p.base_unit) AS presentation
            FROM dbo.Product p
            JOIN dbo.Category c ON c.id = p.category_id
            LEFT JOIN dbo.ClinicalMedicationDetail d ON d.product_id = p.id
            OUTER APPLY (
                SELECT TOP 1 pp.name AS presentation
                FROM dbo.ProductPresentation pp
                WHERE pp.product_id = p.id AND pp.is_active = 1
                ORDER BY pp.is_default DESC, pp.id ASC
            ) pp
            WHERE p.record_status = 'Active'
              AND (d.product_id IS NOT NULL OR UPPER(LTRIM(RTRIM(c.name))) = 'MEDICAMENTOS')
              AND (p.name LIKE ? OR p.product_code LIKE ? OR COALESCE(d.active_ingredient, '') LIKE ?)
            ORDER BY p.name
            """,
            (r, n) -> new MedicationDetail(
                r.getLong("id"),
                r.getString("name"),
                r.getString("active_ingredient"),
                r.getString("concentration"),
                r.getString("presentation")
            ),
            term, term, term
        );
    }

    @Override
    public List<VaccineProductOption> searchVaccines(String text) {
        String term = "%" + (text == null ? "" : text.trim()) + "%";
        return jdbc.query(
            """
            SELECT p.id, p.product_code, p.name, c.name AS category_name,
                   p.base_unit, (p.stock_quantity - p.reserved_quantity) AS available_stock,
                   p.price,
                   pp.sale_price, pp.quantity_in_base
            FROM dbo.Product p
            JOIN dbo.Category c ON c.id = p.category_id
            OUTER APPLY (
                SELECT TOP 1 sale_price, quantity_in_base
                FROM dbo.ProductPresentation
                WHERE product_id = p.id AND is_active = 1
                ORDER BY is_default DESC, id ASC
            ) pp
            WHERE p.record_status = 'Active'
              AND UPPER(c.name) IN ('VACUNAS', 'MEDICAMENTOS')
              AND (p.name LIKE ? OR p.product_code LIKE ? OR c.name LIKE ?)
            ORDER BY CASE WHEN UPPER(c.name) = 'VACUNAS' THEN 0 ELSE 1 END, p.name
            """,
            (r, n) -> {
                BigDecimal presPrice = r.getBigDecimal("sale_price");
                if (presPrice == null) presPrice = r.getBigDecimal("price");
                Integer qtyBase = (Integer) r.getObject("quantity_in_base");
                int q = (qtyBase == null || qtyBase <= 0) ? 1 : qtyBase;
                BigDecimal unitPrice = presPrice == null
                        ? BigDecimal.ZERO
                        : presPrice.divide(BigDecimal.valueOf(q), 2, RoundingMode.HALF_UP);
                return new VaccineProductOption(
                    r.getLong("id"),
                    r.getString("product_code"),
                    r.getString("name"),
                    r.getString("category_name"),
                    r.getString("base_unit"),
                    Math.max(0, r.getInt("available_stock")),
                    unitPrice,
                    presPrice == null ? BigDecimal.ZERO : presPrice,
                    q
                );
            },
            term, term, term
        );
    }
}
