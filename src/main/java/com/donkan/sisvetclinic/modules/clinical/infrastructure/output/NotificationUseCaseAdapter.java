package com.donkan.sisvetclinic.modules.clinical.infrastructure.output;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.donkan.sisvetclinic.modules.clinical.service.port.out.NotificationDispatchPort;
import com.donkan.sisvetclinic.modules.notification.service.port.in.NotificationUseCase;
import com.donkan.sisvetclinic.modules.notification.service.dto.request.NotificationRequest;

@Component
public class NotificationUseCaseAdapter implements NotificationDispatchPort {

    private final ObjectProvider<NotificationUseCase> notifications;

    public NotificationUseCaseAdapter(
        ObjectProvider<NotificationUseCase> notifications
    ) {
        this.notifications = notifications;
    }

    @Override
    public boolean send(
        long recipientId,
        String key,
        String subject,
        String message
    ) {
        NotificationUseCase useCase = notifications.getIfAvailable();

        if (useCase == null) {
            return false;
        }

        var result = useCase.sendNotification(
            new NotificationRequest(
                recipientId, subject, message, "EMAIL", key
            )
        );

        return result != null && result.success();
    }

    @Override
    public String sendWithDetail(
        long recipientId,
        String key,
        String subject,
        String message
    ) {
        NotificationUseCase useCase = notifications.getIfAvailable();

        if (useCase == null) {
            return "El módulo de notificaciones no está disponible.";
        }

        var result = useCase.sendNotification(
            new NotificationRequest(
                recipientId, subject, message, "EMAIL", key
            )
        );

        if (result == null) {
            return "No se obtuvo respuesta del servicio de notificaciones.";
        }
        if (!result.success()) {
            throw new IllegalStateException(result.message());
        }
        return result.message();
    }
}
