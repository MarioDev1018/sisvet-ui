package com.donkan.sisvetclinic.modules.commercial.infrastructure.output.external;

import com.donkan.sisvetclinic.modules.commercial.service.port.out.CommercialNotificationPort;
import com.donkan.sisvetclinic.modules.notification.service.dto.request.NotificationRequest;
import com.donkan.sisvetclinic.modules.notification.service.port.in.NotificationUseCase;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Bridges commercial receipts to the notification module after transaction commit.
 */
@Component
public class CommercialNotificationAdapter implements CommercialNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(CommercialNotificationAdapter.class);

    private final ObjectProvider<NotificationUseCase> notifications;

    public CommercialNotificationAdapter(ObjectProvider<NotificationUseCase> notifications) {
        this.notifications = notifications;
    }

    @Override
    public void sendReceipt(Long customerId, String idempotencyKey, String subject, String message) {
        if (customerId == null || customerId <= 0 || subject == null || message == null) {
            return;
        }

        Runnable dispatch = () -> CompletableFuture.runAsync(() -> {
            try {
                NotificationUseCase useCase = notifications.getIfAvailable();
                if (useCase == null) {
                    return;
                }
                useCase.sendNotification(new NotificationRequest(
                        customerId,
                        subject,
                        message,
                        "EMAIL",
                        idempotencyKey
                ));
            } catch (RuntimeException ex) {
                log.warn("No se pudo despachar el comprobante por correo [cliente={}, clave={}]: {}",
                        customerId, idempotencyKey, ex.getMessage());
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }

        dispatch.run();
    }
}
