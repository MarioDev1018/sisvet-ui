package com.donkan.sisvetclinic.modules.payment.service.port.in;

import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentCredentials;
import com.donkan.sisvetclinic.modules.payment.service.dto.request.PaymentRequest;
import com.donkan.sisvetclinic.modules.payment.service.dto.response.PaymentResult;

public interface PaymentUseCase {

    PaymentResult initiatePayment(PaymentRequest request);

    PaymentResult submitCredentials(String paymentId, PaymentCredentials credentials);

    PaymentResult verifyPayment(String paymentId);

    PaymentResult findPayment(String paymentId);

    boolean supportsProvider(String provider);
}