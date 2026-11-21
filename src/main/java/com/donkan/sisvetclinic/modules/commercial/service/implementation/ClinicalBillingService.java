package com.donkan.sisvetclinic.modules.commercial.service.implementation;

import com.donkan.sisvetclinic.modules.commercial.domain.ClinicalCharge;
import com.donkan.sisvetclinic.modules.commercial.domain.Customer;
import com.donkan.sisvetclinic.modules.commercial.domain.PaymentMethod;
import com.donkan.sisvetclinic.modules.commercial.domain.Sale;
import com.donkan.sisvetclinic.modules.commercial.service.dto.request.ClinicalPaymentRequest;
import com.donkan.sisvetclinic.modules.commercial.service.dto.response.SaleResult;
import com.donkan.sisvetclinic.modules.commercial.service.port.in.ClinicalBillingUseCase;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.ClinicalChargePort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.CustomerRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentMethodRepositoryPort;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.SaleRepositoryPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

public class ClinicalBillingService implements ClinicalBillingUseCase {

    private final ClinicalChargePort charges;
    private final SaleRepositoryPort sales;
    private final CustomerRepositoryPort customers;
    private final PaymentMethodRepositoryPort paymentMethods;

    public ClinicalBillingService(ClinicalChargePort charges,
                                  SaleRepositoryPort sales,
                                  CustomerRepositoryPort customers,
                                  PaymentMethodRepositoryPort paymentMethods) {

        this.charges = charges;
        this.sales = sales;
        this.customers = customers;
        this.paymentMethods = paymentMethods;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalCharge> getPendingCharges(Long consultationId) {

        CommercialValidation.requireId(consultationId, "Consulta");

        return charges.findPendingByConsultation(consultationId);
    }

    @Override
    @Transactional
    public SaleResult registerClinicalPayment(Long userId, ClinicalPaymentRequest request) {

        CommercialValidation.requireId(userId, "Usuario");

        if (request == null) {
            throw new IllegalArgumentException("El cobro clínico es obligatorio.");
        }

        CommercialValidation.requireId(request.consultationId(), "Consulta");
        CommercialValidation.requireId(request.customerId(), "Cliente");
        CommercialValidation.requireId(request.paymentMethodId(), "Método de pago");

        String operationKey = CommercialValidation.requireOperationKey(request.operationKey());

        if (request.chargeIds() == null || request.chargeIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar conceptos para cobrar.");
        }

        Set<Long> uniqueIds = new HashSet<>();

        for (Long chargeId : request.chargeIds()) {

            CommercialValidation.requireId(chargeId, "Concepto clínico");

            if (!uniqueIds.add(chargeId)) {
                throw new IllegalArgumentException("Se seleccionó un concepto repetido.");
            }
        }

        List<Long> selectedIds = new ArrayList<>(uniqueIds);
        selectedIds.sort(Long::compareTo);

        String fingerprint = CommercialValidation.fingerprint(
                "CLINICAL|" + userId + "|" + request.consultationId() + "|"
                + request.customerId() + "|" + request.paymentMethodId() + "|"
                + selectedIds
        );

        Sale previous = sales.findByOperationKey(operationKey).orElse(null);

        if (previous != null) {

            if (!fingerprint.equals(previous.getRequestFingerprint())) {
                throw new IllegalStateException("La clave de operación ya se utilizó con otros datos.");
            }

            return buildResult(previous);
        }

        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new NoSuchElementException("El cliente no existe."));

        if (!customer.isActive()) {
            throw new IllegalStateException("El cliente está inactivo.");
        }

        PaymentMethod paymentMethod = paymentMethods.findById(request.paymentMethodId())
                .orElseThrow(() -> new NoSuchElementException("El método de pago no existe."));

        if (!paymentMethod.isActive()) {
            throw new IllegalStateException("El método de pago está inactivo.");
        }

        List<ClinicalCharge> pendingCharges = charges.findPendingByConsultation(request.consultationId());

        Map<Long, ClinicalCharge> availableCharges = new HashMap<>();

        for (ClinicalCharge charge : pendingCharges) {
            availableCharges.put(charge.getId(), charge);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Long chargeId : selectedIds) {

            ClinicalCharge charge = availableCharges.get(chargeId);

            if (charge == null || charge.isCharged()) {
                throw new IllegalStateException("El concepto clínico ya no está pendiente.");
            }

            if (!request.consultationId().equals(charge.getConsultationId())) {
                throw new IllegalStateException("El concepto no pertenece a la consulta.");
            }

            BigDecimal amount = CommercialValidation.requireMoney(charge.getAmount());
            total = total.add(amount);
        }

        Sale sale = new Sale();

        sale.setUserId(userId);
        sale.setCustomerId(customer.getId());
        sale.setPaymentMethodId(paymentMethod.getId());
        sale.setConsultationId(request.consultationId());
        sale.setSaleDatetime(LocalDateTime.now());
        sale.setRecordStatus("Active");
        sale.setOperationType("CLINICAL");
        sale.setOperationKey(operationKey);
        sale.setRequestFingerprint(fingerprint);
        sale.setTotalAmount(total);

        Sale savedSale = sales.save(sale);

        charges.markAsCharged(selectedIds, savedSale.getId());

        return buildResult(savedSale);
    }

    private SaleResult buildResult(Sale sale) {

        return new SaleResult(
                sale.getId(),
                sale.getTotalAmount(),
                sale.getSaleDatetime(),
                sale.getRecordStatus()
        );
    }
}