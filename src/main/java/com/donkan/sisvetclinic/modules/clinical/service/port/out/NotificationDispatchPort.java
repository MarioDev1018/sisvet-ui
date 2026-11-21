package com.donkan.sisvetclinic.modules.clinical.service.port.out;

public interface NotificationDispatchPort {

    boolean send(
        long recipientId,
        String key,
        String subject,
        String message
    );

    default String sendWithDetail(
        long recipientId,
        String key,
        String subject,
        String message
    ) {
        boolean ok = send(recipientId, key, subject, message);
        return ok ? "Notificación procesada correctamente." : "No se pudo procesar la notificación.";
    }
}
