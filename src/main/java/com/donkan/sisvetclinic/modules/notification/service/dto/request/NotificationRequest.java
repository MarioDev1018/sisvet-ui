package com.donkan.sisvetclinic.modules.notification.service.dto.request;

public record NotificationRequest(
    long recipientId,
    String subject,
    String message,
    String channel,
    String idempotencyKey
) {}
