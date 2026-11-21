
package com.donkan.sisvetclinic.modules.payment.service.port.out;

public interface QrGeneratorPort {

    String generateBase64(String content, int width, int height);
}
