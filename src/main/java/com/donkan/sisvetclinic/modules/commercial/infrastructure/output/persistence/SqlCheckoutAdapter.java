package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.persistence;

import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutItem;
import com.donkan.sisvetclinic.modules.commercial.domain.CheckoutOperation;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.SaleRequest;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CheckoutRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SqlCheckoutAdapter implements CheckoutRepositoryPort {

    private final JdbcTemplate jdbc;

    private static final String SELECT_CHECKOUT = """
        SELECT operation_key, user_id, customer_id, payment_method_id,
               provider_code, total_amount, request_fingerprint,
               status, payment_id, sale_id
        FROM dbo.CheckoutOperation
        """;

    public SqlCheckoutAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PaymentRoute findPaymentRoute(Long paymentMethodId) {

        if (paymentMethodId == null || paymentMethodId <= 0) {
            throw new IllegalArgumentException("Método de pago inválido.");
        }

        List<PaymentRoute> result = jdbc.query(
                """
                SELECT processing_type, provider_code
                FROM dbo.PaymentMethod
                WHERE id = ? AND is_active = 1
                """,
                (rs, row) -> new PaymentRoute(
                        rs.getString("processing_type"),
                        rs.getString("provider_code")
                ),
                paymentMethodId
        );

        return result.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El método de pago no existe o está inactivo."
                ));
    }

    @Override
    @Transactional
    public CheckoutOperation reserve(Long userId, SaleRequest request, String provider) {

        validateRequest(userId, request);

        String fingerprint = fingerprint(userId, request);

        CheckoutOperation existing = jdbc.query(
                SELECT_CHECKOUT + """
                    WITH (UPDLOCK, HOLDLOCK)
                    WHERE operation_key = ?
                    """,
                (rs, row) -> mapCheckout(rs),
                request.operationKey()
        ).stream().findFirst().orElse(null);

        if (existing != null) {

            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IllegalStateException(
                        "La clave de operación ya fue utilizada con otro contenido."
                );
            }

            return existing;
        }

        PaymentRoute method = findPaymentRoute(request.paymentMethodId());

        if (!"EXTERNAL".equalsIgnoreCase(method.processingType())
                || !provider.equalsIgnoreCase(method.provider())) {
            throw new IllegalArgumentException(
                    "El método no corresponde a una pasarela externa válida."
            );
        }

        Integer customerCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.Customer WHERE id = ? AND record_status = 'Active'",
                Integer.class,
                request.customerId()
        );

        if (customerCount == null || customerCount != 1) {
            throw new IllegalArgumentException("El cliente no existe o está inactivo.");
        }

        List<CheckoutItem> lines = new ArrayList<>();
        Set<Long> usedPresentations = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SaleRequest.Item item : request.items()) {

            if (item == null || item.productId() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("El detalle de venta es inválido.");
            }

            List<PresentationData> presentations = item.presentationId() != null
                    ? jdbc.query(
                            """
                            SELECT pp.id, pp.quantity_in_base, pp.sale_price, pp.is_fractionable, p.base_unit
                            FROM dbo.ProductPresentation pp
                            INNER JOIN dbo.Product p ON p.id = pp.product_id
                            WHERE pp.id = ?
                              AND pp.product_id = ?
                              AND pp.is_active = 1
                              AND p.record_status = 'Active'
                            """,
                            (rs, row) -> new PresentationData(
                                    rs.getLong("id"),
                                    rs.getInt("quantity_in_base"),
                                    rs.getBigDecimal("sale_price"),
                                    rs.getBoolean("is_fractionable"),
                                    rs.getString("base_unit")
                            ),
                            item.presentationId(),
                            item.productId()
                    )
                    : jdbc.query(
                            """
                            SELECT pp.id, pp.quantity_in_base, pp.sale_price, pp.is_fractionable, p.base_unit
                            FROM dbo.ProductPresentation pp
                            INNER JOIN dbo.Product p ON p.id = pp.product_id
                            WHERE pp.product_id = ?
                              AND pp.is_default = 1
                              AND pp.is_active = 1
                              AND p.record_status = 'Active'
                            """,
                            (rs, row) -> new PresentationData(
                                    rs.getLong("id"),
                                    rs.getInt("quantity_in_base"),
                                    rs.getBigDecimal("sale_price"),
                                    rs.getBoolean("is_fractionable"),
                                    rs.getString("base_unit")
                            ),
                            item.productId()
                    );

            PresentationData presentation = presentations.stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El producto o presentación no está disponible."
                    ));

            if (!usedPresentations.add(presentation.id())) {
                throw new IllegalArgumentException(
                        "La presentación está repetida en el carrito."
                );
            }

            if (presentation.quantityInBase() <= 0
                    || presentation.salePrice() == null
                    || presentation.salePrice().signum() <= 0) {
                throw new IllegalArgumentException(
                        "La presentación tiene una equivalencia o precio inválido."
                );
            }

            boolean fractional = item.requestedBaseQuantity() != null;
            int baseQuantity;
            BigDecimal unitPrice;

            if (fractional) {

                if (!presentation.fractionable()
                        || "UNIT".equalsIgnoreCase(presentation.baseUnit())
                        || item.quantity() != 1
                        || item.requestedBaseQuantity() <= 0) {
                    throw new IllegalArgumentException(
                            "La venta fraccionada no es válida."
                    );
                }

                baseQuantity = item.requestedBaseQuantity();

                unitPrice = presentation.salePrice()
                        .multiply(BigDecimal.valueOf(baseQuantity))
                        .divide(
                                BigDecimal.valueOf(presentation.quantityInBase()),
                                2,
                                RoundingMode.HALF_UP
                        );

            } else {

                try {
                    baseQuantity = Math.multiplyExact(
                            item.quantity(),
                            presentation.quantityInBase()
                    );
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException(
                            "La cantidad física supera el límite permitido."
                    );
                }

                unitPrice = presentation.salePrice();
            }

            if (baseQuantity <= 0 || unitPrice.signum() <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad o el importe calculado no es válido."
                );
            }

            BigDecimal subtotal = unitPrice
                    .multiply(BigDecimal.valueOf(item.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            lines.add(new CheckoutItem(
                    item.productId(),
                    presentation.id(),
                    item.quantity(),
                    item.requestedBaseQuantity(),
                    baseQuantity,
                    unitPrice,
                    subtotal
            ));

            total = total.add(subtotal);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        if (total.signum() <= 0) {
            throw new IllegalArgumentException("El importe total es inválido.");
        }

        jdbc.update(
                """
                INSERT INTO dbo.CheckoutOperation (
                    operation_key, user_id, customer_id, payment_method_id,
                    provider_code, total_amount, request_fingerprint, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_PAYMENT')
                """,
                request.operationKey(),
                userId,
                request.customerId(),
                request.paymentMethodId(),
                provider,
                total,
                fingerprint
        );

        List<CheckoutItem> sortedForLock = lines.stream()
                .sorted(Comparator.comparing(CheckoutItem::productId)
                        .thenComparing(CheckoutItem::presentationId))
                .toList();

        for (CheckoutItem line : sortedForLock) {

            int updated = jdbc.update(
                    """
                    UPDATE dbo.Product
                    SET reserved_quantity = reserved_quantity + ?
                    WHERE id = ?
                      AND stock_quantity - reserved_quantity >= ?
                      AND record_status = 'Active'
                    """,
                    line.baseQuantity(),
                    line.productId(),
                    line.baseQuantity()
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "Stock insuficiente o producto no disponible: " + line.productId()
                );
            }
        }

        int number = 1;

        for (CheckoutItem line : lines) {
            jdbc.update(
                    """
                    INSERT INTO dbo.CheckoutItem (
                        operation_key, line_number, product_id,
                        presentation_id, quantity, requested_base_quantity,
                        base_quantity, unit_price, subtotal
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    request.operationKey(),
                    number++,
                    line.productId(),
                    line.presentationId(),
                    line.quantity(),
                    line.requestedBaseQuantity(),
                    line.baseQuantity(),
                    line.unitPrice(),
                    line.subtotal()
            );
        }

        return findByKey(request.operationKey()).orElseThrow();
    }

    @Override
    public Optional<CheckoutOperation> findByKey(String operationKey) {

        return jdbc.query(
                SELECT_CHECKOUT + " WHERE operation_key = ?",
                (rs, row) -> mapCheckout(rs),
                operationKey
        ).stream().findFirst();
    }

    @Override
    public CheckoutOperation lockByKey(String operationKey) {

        return jdbc.query(
                SELECT_CHECKOUT + """
                    WITH (UPDLOCK, HOLDLOCK)
                    WHERE operation_key = ?
                    """,
                (rs, row) -> mapCheckout(rs),
                operationKey
        ).stream().findFirst().orElseThrow(
                () -> new NoSuchElementException("El checkout no existe.")
        );
    }

    @Override
    public List<CheckoutItem> findItems(String operationKey) {

        return jdbc.query(
                """
                SELECT product_id, presentation_id, quantity,
                       requested_base_quantity, base_quantity,
                       unit_price, subtotal
                FROM dbo.CheckoutItem
                WHERE operation_key = ?
                ORDER BY line_number
                """,
                (rs, row) -> {
                    int requested = rs.getInt("requested_base_quantity");
                    Integer fractional = rs.wasNull() ? null : requested;

                    return new CheckoutItem(
                            rs.getLong("product_id"),
                            rs.getLong("presentation_id"),
                            rs.getInt("quantity"),
                            fractional,
                            rs.getInt("base_quantity"),
                            rs.getBigDecimal("unit_price"),
                            rs.getBigDecimal("subtotal")
                    );
                },
                operationKey
        );
    }

    @Override
    public void linkPayment(String operationKey, String paymentId) {

        int updated = jdbc.update(
                """
                UPDATE dbo.CheckoutOperation
                SET payment_id = ?,
                    updated_at = SYSUTCDATETIME()
                WHERE operation_key = ?
                  AND status = 'PENDING_PAYMENT'
                  AND (payment_id IS NULL OR payment_id = ?)
                """,
                paymentId,
                operationKey,
                paymentId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "El checkout cambió o ya tiene otra operación de pago."
            );
        }
    }

    @Override
    public void releaseReservation(String operationKey) {

        List<CheckoutItem> sortedItems = findItems(operationKey).stream()
                .sorted(Comparator.comparing(CheckoutItem::productId)
                        .thenComparing(CheckoutItem::presentationId))
                .toList();

        for (CheckoutItem line : sortedItems) {

            int updated = jdbc.update(
                    """
                    UPDATE dbo.Product
                    SET reserved_quantity = reserved_quantity - ?
                    WHERE id = ?
                      AND reserved_quantity >= ?
                    """,
                    line.baseQuantity(),
                    line.productId(),
                    line.baseQuantity()
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "No se pudo liberar la reserva del producto " + line.productId()
                );
            }
        }
    }

    @Override
    public void markCompleted(String operationKey, Long saleId) {

        int updated = jdbc.update(
                """
                UPDATE dbo.CheckoutOperation
                SET status = 'COMPLETED',
                    sale_id = ?,
                    updated_at = SYSUTCDATETIME()
                WHERE operation_key = ?
                  AND status IN ('PENDING_PAYMENT', 'RECOVERY_REQUIRED')
                  AND sale_id IS NULL
                """,
                saleId,
                operationKey
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "El checkout no pudo finalizarse."
            );
        }
    }

    @Override
    public void markRecoveryRequired(String operationKey) {

        jdbc.update(
                """
                UPDATE dbo.CheckoutOperation
                SET status = 'RECOVERY_REQUIRED',
                    updated_at = SYSUTCDATETIME()
                WHERE operation_key = ?
                  AND status = 'PENDING_PAYMENT'
                """,
                operationKey
        );
    }

    private void validateRequest(Long userId, SaleRequest request) {

        if (userId == null || userId <= 0 || request == null
                || request.customerId() == null || request.customerId() <= 0
                || request.paymentMethodId() == null || request.paymentMethodId() <= 0
                || request.operationKey() == null
                || !request.operationKey().matches("[0-9a-fA-F-]{36}")
                || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La solicitud comercial es inválida.");
        }
    }

    private String fingerprint(Long userId, SaleRequest request) {

        StringBuilder text = new StringBuilder();

        text.append(userId).append('|')
                .append(request.customerId()).append('|')
                .append(request.paymentMethodId()).append('|');

        request.items().stream()
                .sorted(Comparator.comparing(
                        item -> item.presentationId() == null ? -1 : item.presentationId()
                ))
                .forEach(item -> text.append(item.productId()).append(':')
                        .append(item.presentationId()).append(':')
                        .append(item.quantity()).append(':')
                        .append(item.requestedBaseQuantity()).append(';'));

        try {

            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(text.toString().getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(bytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular la huella.", exception);
        }
    }

    private CheckoutOperation mapCheckout(ResultSet rs) throws SQLException {

        long saleId = rs.getLong("sale_id");
        Long confirmedSaleId = rs.wasNull() ? null : saleId;

        return new CheckoutOperation(
                rs.getString("operation_key"),
                rs.getLong("user_id"),
                rs.getLong("customer_id"),
                rs.getLong("payment_method_id"),
                rs.getString("provider_code"),
                rs.getBigDecimal("total_amount"),
                rs.getString("request_fingerprint"),
                rs.getString("status"),
                rs.getString("payment_id"),
                confirmedSaleId
        );
    }

    private record PresentationData(
            Long id,
            int quantityInBase,
            BigDecimal salePrice,
            boolean fractionable,
            String baseUnit
    ) {
    }
    
    @Override
    @Transactional
    public void cancelFailedCheckout(String operationKey, String paymentId) {

        CheckoutOperation checkout = lockByKey(operationKey);

        if ("CANCELLED".equals(checkout.status())) {
            return;
        }

        if (!"PENDING_PAYMENT".equals(checkout.status())
                || !paymentId.equals(checkout.paymentId())) {
            throw new IllegalStateException(
                    "No se puede cancelar esta operación."
            );
        }

        releaseReservation(operationKey);

        int updated = jdbc.update(
                """
                UPDATE dbo.CheckoutOperation
                SET status = 'CANCELLED',
                    updated_at = SYSUTCDATETIME()
                WHERE operation_key = ?
                  AND status = 'PENDING_PAYMENT'
                """,
                operationKey
        );

        if (updated != 1) {
            throw new IllegalStateException("No se pudo cancelar el checkout.");
        }
    }
    
    @Override
    @Transactional
    public void cancelSimulatedRecoveryCheckout(String operationKey, String paymentId) {

        CheckoutOperation checkout = lockByKey(operationKey);

        if ("CANCELLED".equals(checkout.status())) {
            return;
        }

        if (!"RECOVERY_REQUIRED".equals(checkout.status())
                || !"YAPE".equalsIgnoreCase(checkout.provider())
                || checkout.saleId() != null
                || paymentId == null
                || !paymentId.equals(checkout.paymentId())) {

            throw new IllegalStateException(
                    "La operación no cumple las condiciones de cancelación."
            );
        }

        Integer validPayment = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM dbo.PaymentOrder WITH (UPDLOCK, HOLDLOCK)
                WHERE id = ?
                  AND operation_reference = ?
                  AND provider = 'YAPE'
                  AND status = 'UNKNOWN'
                  AND provider_order_id IS NULL
                  AND capture_reference IS NULL
                  AND charged_amount IS NULL
                """,
                Integer.class,
                paymentId,
                "CHECKOUT-" + operationKey
        );

        if (validPayment == null || validPayment != 1) {
            throw new IllegalStateException(
                    "El pago tiene información que impide cancelarlo automáticamente."
            );
        }

        Integer existingSales = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM dbo.Sale WITH (UPDLOCK, HOLDLOCK)
                WHERE operation_key = ?
                """,
                Integer.class,
                operationKey
        );

        if (existingSales == null || existingSales != 0) {
            throw new IllegalStateException(
                    "La operación ya tiene una venta registrada."
            );
        }

        // La liberación y la cancelación pertenecen a la misma transacción.
        releaseReservation(operationKey);

        int updated = jdbc.update(
                """
                UPDATE dbo.CheckoutOperation
                SET status = 'CANCELLED',
                    updated_at = SYSUTCDATETIME()
                WHERE operation_key = ?
                  AND payment_id = ?
                  AND status = 'RECOVERY_REQUIRED'
                  AND sale_id IS NULL
                """,
                operationKey,
                paymentId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "No se pudo completar la cancelación del checkout."
            );
        }
    }
}