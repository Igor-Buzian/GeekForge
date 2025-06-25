// com.example.spring.service.payment.PayPalPaymentProcessor.java
package com.example.spring.service.payment;

import com.paypal.http.HttpResponse;
import com.paypal.http.serializer.Json;
import com.paypal.orders.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PayPalPaymentProcessor implements PaymentProcessor {

    private final com.paypal.core.PayPalHttpClient payPalClient; // HTTP-клиент PayPal

    public PayPalPaymentProcessor(
            @Value("${paypal.clientId}") String clientId,
            @Value("${paypal.clientSecret}") String clientSecret,
            @Value("${paypal.mode}") String mode) {
        // Настройка среды PayPal
        com.paypal.core.PayPalEnvironment environment;
        if ("live".equalsIgnoreCase(mode)) {
            environment = new com.paypal.core.PayPalEnvironment.Live(clientId, clientSecret);
        } else {
            environment = new com.paypal.core.PayPalEnvironment.Sandbox(clientId, clientSecret);
        }
        this.payPalClient = new com.paypal.core.PayPalHttpClient(environment);
    }

    /**
     * Этот метод в интерфейсе PaymentProcessor будет использоваться для "захвата" уже созданного заказа.
     * Он НЕ используется для изначального создания заказа.
     */
    @Override
    public String processPayment(BigDecimal amount, String currency, String paypalOrderId, String customerEmail) throws PaymentException {
        // Мы используем этот метод для захвата средств по существующему PayPal Order ID
        return capturePayPalOrder(paypalOrderId);
    }

    @Override
    public PaymentGatewayType getType() {
        return PaymentGatewayType.PAYPAL;
    }

    /**
     * Создает заказ на стороне PayPal.
     * Этот метод будет вызываться с вашего бэкенда, когда фронтенд готов к инициированию платежа.
     * @param amount Сумма заказа.
     * @param currency Валюта.
     * @param returnUrl URL, куда PayPal вернет пользователя после завершения оплаты.
     * @param cancelUrl URL, куда PayPal вернет пользователя, если он отменит оплату.
     * @return ID заказа PayPal.
     * @throws PaymentException если произошла ошибка при создании заказа.
     */
    public String createPayPalOrder(BigDecimal amount, String currency, String returnUrl, String cancelUrl) throws PaymentException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE"); // Указываем, что мы хотим захватить средства

        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown().currencyCode(currency).value(amount.toPlainString());
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest().amountWithBreakdown(amountBreakdown);
        orderRequest.purchaseUnits(Collections.singletonList(purchaseUnit));

        ApplicationContext applicationContext = new ApplicationContext()
                .brandName("Your Store Name") // Название вашего магазина
                .landingPage("BILLING") // Или "LOGIN", "NO_PREFERENCE"
                .userAction("PAY_NOW") // "PAY_NOW" для немедленной оплаты, "CONTINUE" для подтверждения на странице
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl);
        orderRequest.applicationContext(applicationContext);

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

        try {
            HttpResponse<Order> response = payPalClient.execute(request);
            if (response.statusCode() == 201) { // 201 Created
                Order order = response.result();
                System.out.println("PayPal Order ID: " + order.id());
                // Возвращаем ID заказа PayPal, чтобы фронтенд мог использовать его для инициации платежа
                return order.id();
            } else {
                throw new PaymentException(HttpStatus.BAD_REQUEST, "Failed to create PayPal order. Status: " + response.statusCode() + ", Response: " + new JSONObject(new Json().serialize(response.result())).toString(4));
            }
        } catch (IOException e) {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with PayPal API: " + e.getMessage(), e);
        }
    }

    /**
     * Захватывает средства по существующему PayPal Order ID.
     * Этот метод вызывается после того, как пользователь успешно завершил оплату на стороне PayPal (фронтенд возвращает orderId).
     * @param paypalOrderId ID заказа PayPal, который был создан ранее.
     * @return ID транзакции PayPal.
     * @throws PaymentException если произошла ошибка при захвате средств.
     */
    public String capturePayPalOrder(String paypalOrderId) throws PaymentException {
        OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
        request.requestBody(new OrderActionRequest()); // Пустой запрос, если не нужно дополнительных параметров

        try {
            HttpResponse<Order> response = payPalClient.execute(request);

            if (response.statusCode() == 201) { // 201 Created (для capture)
                Order order = response.result();
                // Ищем successful capture
                for (PurchaseUnit purchaseUnit : order.purchaseUnits()) {
                    for (Capture capture : purchaseUnit.payments().captures()) {
                        if ("COMPLETED".equals(capture.status())) {
                            System.out.println("PayPal Capture ID: " + capture.id());
                            return capture.id(); // Возвращаем ID успешного захвата
                        }
                    }
                }
                throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal order captured, but no COMPLETED capture found.");
            } else {
                String errorDetails = "";
                if (response.result() != null && response.result().details() != null) {
                    errorDetails = response.result().details().stream()
                            .map(error -> error.issue() + ": " + error.description())
                            .collect(Collectors.joining("; "));
                }
                throw new PaymentException(HttpStatus.BAD_REQUEST, "Failed to capture PayPal order. Status: " + response.statusCode() + ", Details: " + errorDetails);
            }
        } catch (IOException e) {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with PayPal API during capture: " + e.getMessage(), e);
        } catch (NoSuchElementException e) {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "No purchase units or captures found in PayPal response: " + e.getMessage(), e);
        }
    }
}