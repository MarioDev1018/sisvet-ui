package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class CommercialValidation {

    private CommercialValidation() {
    }

    static Long requireId(Long id, String field) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(field + " inválido.");
        }

        return id;
    }

    static String requireText(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }

        return value.trim();
    }

    static int requireQuantity(int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        return quantity;
    }

    static BigDecimal requireMoney(BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero.");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe admite como máximo dos decimales.");
        }

        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    static String requireOperationKey(String operationKey) {

        String key = requireText(operationKey, "La clave de operación");

        if (key.length() > 100) {
            throw new IllegalArgumentException("La clave de operación es demasiado larga.");
        }

        return key;
    }

    static String fingerprint(String value) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException("No se pudo generar la huella de operación.", exception);
        }
    }
}