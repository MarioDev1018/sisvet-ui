package com.donkan.sisvetclinic.modules.notification.service.port.out;

import com.donkan.sisvetclinic.modules.notification.service.dto.response.NotificationResult;

public interface RecipientDirectoryPort {

    RecipientContact findCustomerContact(long customerId);

    NotificationResult findByIdempotencyKey(String key);

    long saveLog(long recipientId, String email, String channel, String key,
                 String subject, String body, boolean sentOk, String detail);

    record RecipientContact(long id, String fullName, String email) {}
}
