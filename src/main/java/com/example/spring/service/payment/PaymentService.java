// src/main/java/com/example/spring/service/payment/PaymentService.java
package com.example.spring.service.payment;

import com.example.spring.entity.User;

import java.math.BigDecimal;
import java.util.Map;

// Defines a general exception for payment system operations
// or use RuntimeException if it's preferred not to force the calling code to handle specific exceptions
class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentProcessingException(String message) {
        super(message);
    }
}

public interface PaymentService {

    /**
     * Returns a unique identifier for this type of payment service (e.g., "paypal", "stripe").
     * This will be used by the factory to select the correct implementation.
     */
    String getServiceType();

    /**
     * Initiates a payment.
     * @param totalAmount The total amount of the payment.
     * @param currency The currency (e.g., "MDL").
     * @param description The description of the payment.
     * @param cancelUrl The URL for redirection if the payment is canceled.
     * @param successUrl The URL for redirection upon successful payment.
     * @return A map containing the URL to redirect the user to the payment system's page (key "redirect_url").
     * In the future, a more complex DTO can be returned containing status, transaction ID, and URL.
     * @throws PaymentProcessingException If an error occurs during payment creation.
     */
    Map<String, String> initiatePayment(BigDecimal totalAmount, String currency, String description, String cancelUrl, String successUrl);

    /**
     * Executes a payment after approval from the payment gateway.
     * @param paymentId The ID of the payment.
     * @param payerId The ID of the payer.
     * @param user The authenticated user performing the payment.
     * @return A map containing details about the executed payment, such as transaction ID.
     * @throws PaymentProcessingException If an error occurs during payment execution.
     */
    Map<String, String> executePayment(String paymentId, String payerId, User user); // <-- Added User user
}