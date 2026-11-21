
package com.donkan.sisvetclinic.modules.payment.infrastructure.config;

import com.donkan.sisvetclinic.modules.payment.service.implementation.PaymentService;
import com.donkan.sisvetclinic.modules.payment.service.port.out.ImmediatePaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentGatewayPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentRepositoryPort;
import com.donkan.sisvetclinic.modules.payment.service.port.out.QrGeneratorPort;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentBeansConfig {

    @Bean
    public PaymentService paymentService(
            PaymentRepositoryPort payments,
            QrGeneratorPort qrGenerator,
            List<PaymentGatewayPort> approvalGateways,
            List<ImmediatePaymentGatewayPort> immediateGateways) {

        return new PaymentService(
                payments, qrGenerator, approvalGateways, immediateGateways
        );
    }
}
