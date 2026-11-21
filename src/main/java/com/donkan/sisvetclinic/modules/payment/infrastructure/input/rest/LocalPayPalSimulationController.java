package com.donkan.sisvetclinic.modules.payment.infrastructure.input.rest;

import com.donkan.sisvetclinic.modules.payment.infrastructure.output.external.paypal.LocalPayPalAdapter;
import com.donkan.sisvetclinic.modules.payment.service.port.out.PaymentRepositoryPort;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@ConditionalOnProperty(
        name = "sisvet.payment.paypal.simulated",
        havingValue = "true",
        matchIfMissing = true
)
public class LocalPayPalSimulationController {

    private final LocalPayPalAdapter paypal;
    private final PaymentRepositoryPort payments;

    public LocalPayPalSimulationController(LocalPayPalAdapter paypal,
                                          PaymentRepositoryPort payments) {

        this.paypal = paypal;
        this.payments = payments;
    }

    @GetMapping(
            value = "/simulacion/paypal-checkout/{orderId}",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public String checkout(@PathVariable String orderId,
                           @RequestParam(defaultValue = "false") boolean approved,
                           HttpServletRequest request) {

        validateOrder(orderId);

        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrf == null) {
            csrf = (CsrfToken) request.getAttribute("_csrf");
        }

        if (csrf == null) {
            throw new IllegalStateException("No se pudo obtener el token CSRF.");
        }

        String message = approved
                ? "Aprobación simulada registrada. Regresa a SISVET."
                : "Confirma esta operación de prueba.";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PayPal - Simulación</title>
                <style>
                    body { font-family: Arial, sans-serif; background: #eef3f8; padding: 24px; }
                    main { background: white; max-width: 480px; margin: auto; padding: 28px; border-radius: 12px; }
                    button { background: #0070ba; color: white; border: 0; border-radius: 6px; padding: 14px 22px; }
                    .note { color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <main>
                    <h2>PayPal - Simulación local</h2>
                    <p>%s</p>
                    <p class="note">Esto no realiza un pago real.</p>
                    <form method="post" action="/simulacion/paypal-checkout/%s/approve">
                        <input type="hidden" name="%s" value="%s">
                        <button type="submit">Aprobar pago simulado</button>
                    </form>
                    <p class="note">Orden: %s</p>
                </main>
            </body>
            </html>
            """.formatted(
                message,
                orderId,
                csrf.getParameterName(),
                csrf.getToken(),
                orderId
        );
    }

    @PostMapping("/simulacion/paypal-checkout/{orderId}/approve")
    public String approve(@PathVariable String orderId) {

        validateOrder(orderId);

        paypal.approveOrder(orderId);

        return "redirect:/simulacion/paypal-checkout/"
                + orderId + "?approved=true";
    }

    private void validateOrder(String orderId) {

        if (orderId == null || !orderId.matches("SIM-PP-[A-F0-9-]{36}")) {
            throw new IllegalArgumentException("Identificador inválido.");
        }

        if (payments.findByProviderOrderId(orderId).isEmpty()) {
            throw new IllegalArgumentException("La orden no existe.");
        }
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseBody
    public ResponseEntity<String> handleSimulationError(RuntimeException exception) {
        HttpStatus status = exception instanceof IllegalArgumentException
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CONFLICT;
        String detail = exception.getMessage() != null
                ? exception.getMessage()
                : "No se pudo procesar la simulación.";
        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <title>PayPal - Simulación</title>
                <style>
                    body { font-family: Arial, sans-serif; background: #eef3f8; padding: 24px; }
                    main { background: white; max-width: 480px; margin: auto; padding: 28px; border-radius: 12px; }
                    .error { color: #b83232; font-weight: bold; }
                </style>
            </head>
            <body>
                <main>
                    <h2>PayPal - Simulación local</h2>
                    <p class="error">%s</p>
                </main>
            </body>
            </html>
            """.formatted(detail);
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}