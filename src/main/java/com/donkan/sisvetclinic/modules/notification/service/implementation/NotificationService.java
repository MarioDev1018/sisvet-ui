package com.donkan.sisvetclinic.modules.notification.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.donkan.sisvetclinic.modules.notification.service.dto.request.NotificationRequest;
import com.donkan.sisvetclinic.modules.notification.service.dto.response.NotificationResult;
import com.donkan.sisvetclinic.modules.notification.service.port.in.NotificationUseCase;
import com.donkan.sisvetclinic.modules.notification.service.port.out.RecipientDirectoryPort;
import com.donkan.sisvetclinic.modules.notification.service.port.out.RecipientDirectoryPort.RecipientContact;

@Service
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RecipientDirectoryPort recipients;
    private final String mailFrom;
    private final String mailPassword;

    @Autowired
    public NotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            RecipientDirectoryPort recipients,
            @Value("${sisvet.notification.mail.from:${spring.mail.username:}}") String mailFrom,
            @Value("${spring.mail.password:}") String mailPassword) {
        this.mailSenderProvider = mailSenderProvider;
        this.recipients = recipients;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.mailPassword = mailPassword == null ? "" : mailPassword.trim();
    }

    @Override
    public NotificationResult sendNotification(NotificationRequest request) {
        if (request == null || request.recipientId() <= 0
                || request.subject() == null || request.subject().isBlank()
                || request.message() == null || request.message().isBlank()) {
            return new NotificationResult(0, false, "Datos de notificación incompletos.");
        }

        String key = request.idempotencyKey();
        if (key != null && !key.isBlank() && recipients != null) {
            NotificationResult existing = recipients.findByIdempotencyKey(key);
            if (existing != null) {
                return existing;
            }
        }

        RecipientContact contact = recipients == null ? null : recipients.findCustomerContact(request.recipientId());
        String email = contact == null || contact.email() == null ? null : contact.email().trim();
        String channel = request.channel() == null || request.channel().isBlank() ? "EMAIL" : request.channel().trim();

        boolean smtpReady = !mailFrom.isBlank() && !mailPassword.isBlank() && mailSenderProvider.getIfAvailable() != null;
        boolean emailValid = email != null && !email.isBlank() && email.contains("@");

        boolean dispatchedByEmail = false;
        String detail;

        if ("EMAIL".equalsIgnoreCase(channel) && smtpReady && emailValid) {
            try {
                JavaMailSender sender = mailSenderProvider.getObject();
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom);
                msg.setTo(email);
                msg.setSubject("[SISVET-CLINIC] " + request.subject().trim());
                String greeting = contact != null && contact.fullName() != null
                        ? "Estimado(a) " + contact.fullName() + ",\n\n"
                        : "Estimado cliente,\n\n";
                msg.setText(greeting
                        + request.message().trim()
                        + "\n\nAtentamente,\nSISVET-CLINIC Veterinaria");
                sender.send(msg);
                dispatchedByEmail = true;
                detail = "Correo enviado a " + email + ".";
                log.info("Correo de notificación enviado [destinatario={}, email={}, clave={}]: {}",
                        request.recipientId(), email, key, request.subject());
            } catch (Exception ex) {
                detail = "No se pudo enviar correo SMTP (" + ex.getMessage() + ").";
                log.error("Error al enviar correo a {}: {}", email, ex.getMessage(), ex);
            }
        } else if (!emailValid) {
            detail = "Cliente sin correo electrónico registrado; recordatorio guardado en bitácora interna.";
            log.info("Notificación registrada sin email [destinatario={}, clave={}]: {}",
                    request.recipientId(), key, request.subject());
        } else if (!smtpReady) {
            detail = "SMTP no configurado (MACP_MAIL_USER / MACP_MAIL_PASSWORD); recordatorio registrado internamente para " + email + ".";
            log.info("Notificación registrada sin credenciales SMTP [destinatario={}, email={}, clave={}]: {}",
                    request.recipientId(), email, key, request.subject());
        } else {
            detail = "Notificación registrada en canal " + channel + ".";
            log.info("Notificación [canal={}, destinatario={}, clave={}]: {}",
                    channel, request.recipientId(), key, request.subject());
        }

        // Si SMTP estuvo configurado pero fallo al enviar a un correo valido, devolvemos success=false para reintento.
        boolean success = !("EMAIL".equalsIgnoreCase(channel) && smtpReady && emailValid) || dispatchedByEmail;

        long id = 0;
        if (recipients != null) {
            id = recipients.saveLog(
                request.recipientId(),
                emailValid ? email : null,
                channel,
                key,
                request.subject().trim(),
                request.message().trim(),
                success,
                detail
            );
        }

        return new NotificationResult(id, success, detail);
    }
}
