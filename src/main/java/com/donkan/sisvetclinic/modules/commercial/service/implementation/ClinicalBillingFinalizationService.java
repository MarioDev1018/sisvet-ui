package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.*;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;

public class ClinicalBillingFinalizationService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final ClinicalChargePort charges;
    private final SaleRepositoryPort sales;
    private final ClinicalPaymentSessionPort sessions;
    private final CommercialNotificationPort notifications;

    public ClinicalBillingFinalizationService(
            ClinicalChargePort charges,
            SaleRepositoryPort sales,
            ClinicalPaymentSessionPort sessions) {
        this(charges, sales, sessions, null);
    }

    public ClinicalBillingFinalizationService(
            ClinicalChargePort charges,
            SaleRepositoryPort sales,
            ClinicalPaymentSessionPort sessions,
            CommercialNotificationPort notifications) {
        this.charges = charges;
        this.sales = sales;
        this.sessions = sessions;
        this.notifications = notifications;
    }

    @Transactional
    public SaleResult finalizePayment(
            Long userId,
            Long consultationId,
            Long attentionId,
            Long customerId,
            Long paymentMethodId,
            List<Long> selectedIds,
            String operationKey,
            String fingerprint,
            boolean completeSession) {

        Sale existing = sales.findByOperationKey(operationKey).orElse(null);
        if (existing != null) {
            if (!fingerprint.equals(existing.getRequestFingerprint())) {
                throw new IllegalStateException("La clave de operación ya se utilizó con otros datos.");
            }
            if (completeSession && sessions != null) {
                sessions.complete(operationKey, existing.getId());
            }
            return buildResult(existing);
        }

        List<ClinicalCharge> pendingCharges = consultationId != null
                ? charges.findPendingByConsultation(consultationId)
                : charges.findPendingByAttention(attentionId);

        Map<Long, ClinicalCharge> availableCharges = new HashMap<>();
        for (ClinicalCharge charge : pendingCharges) {
            availableCharges.put(charge.getId(), charge);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<String> chargeLines = new ArrayList<>();
        for (Long chargeId : selectedIds) {
            ClinicalCharge charge = availableCharges.get(chargeId);
            if (charge == null || charge.isCharged()) {
                throw new IllegalStateException("El concepto clínico #" + chargeId + " ya no está pendiente.");
            }
            BigDecimal amount = CommercialValidation.requireMoney(charge.getAmount());
            total = total.add(amount);
            chargeLines.add("- " + charge.getDescription() + ": S/ " + CommercialReceiptFormatter.formatMoney(amount));
        }

        Sale sale = new Sale();
        sale.setUserId(userId);
        sale.setCustomerId(customerId);
        sale.setPaymentMethodId(paymentMethodId);
        sale.setConsultationId(consultationId);
        sale.setSaleDatetime(LocalDateTime.now(LIMA_ZONE));
        sale.setRecordStatus("Active");
        sale.setOperationType("CLINICAL");
        sale.setOperationKey(operationKey);
        sale.setRequestFingerprint(fingerprint);
        sale.setTotalAmount(total);

        Sale savedSale = sales.save(sale);
        charges.markAsCharged(selectedIds, savedSale.getId());

        if (completeSession && sessions != null) {
            sessions.complete(operationKey, savedSale.getId());
        }

        if (notifications != null) {
            notifications.sendReceipt(
                    customerId,
                    "CLINICAL-RECEIPT-" + savedSale.getId(),
                    "Comprobante de cobro clínico #" + savedSale.getId(),
                    CommercialReceiptFormatter.formatClinicalReceipt(savedSale, consultationId, attentionId, chargeLines)
            );
        }

        return buildResult(savedSale);
    }

    public SaleResult buildResult(Sale sale) {
        return new SaleResult(
                sale.getId(),
                sale.getTotalAmount(),
                sale.getSaleDatetime(),
                sale.getRecordStatus()
        );
    }
}
