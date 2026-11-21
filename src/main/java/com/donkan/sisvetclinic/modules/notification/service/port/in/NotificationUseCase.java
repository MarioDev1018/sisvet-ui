package com.donkan.sisvetclinic.modules.notification.service.port.in;

import com.donkan.sisvetclinic.modules.notification.service.dto.request.NotificationRequest;
import com.donkan.sisvetclinic.modules.notification.service.dto.response.NotificationResult;

public interface NotificationUseCase {

    NotificationResult sendNotification(NotificationRequest request);
}
