package com.donkan.sisvetclinic.modules.commercial.service.port.out;

/**
 * Dispatches purchase and clinical payment confirmation receipts to customers.
 */
public interface CommercialNotificationPort {

    void sendReceipt(Long customerId, String idempotencyKey, String subject, String message);
}
