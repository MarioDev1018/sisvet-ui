
package com.donkan.sisvetclinic.modules.commercial.service.port.in;

import com.donkan.sisvetclinic.modules.commercial.service.dto.request.CheckoutCredentials;
import com.donkan.sisvetclinic.modules.commercial.service.port.out.PaymentProcessingPort.PaymentReceipt;

public interface SalePaymentUseCase {

    PaymentReceipt initiateForSale(Long saleId, String provider);

    PaymentReceipt submitCredentialsForSale(Long saleId, String paymentId, CheckoutCredentials credentials);

    PaymentReceipt verifyForSale(Long saleId, String paymentId);

    PaymentReceipt findForSale(Long saleId, String paymentId);
}
