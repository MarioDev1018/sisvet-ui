
package com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.qr;

import com.donkan.sisvetclinic.modules.payment.service.port.out.QrGeneratorPort;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ZxingQrAdapter implements QrGeneratorPort {

    @Override
    public String generateBase64(String content, int width, int height) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El contenido del QR es obligatorio.");
        }

        try {

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("No se pudo generar el código QR.", exception);
        }
    }
}
