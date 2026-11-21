package com.donkan.sisvetclinic.modules.notification.service.dto.response;

public record NotificationResult(
    long notificationId,
    boolean success,
    String message
) {}
