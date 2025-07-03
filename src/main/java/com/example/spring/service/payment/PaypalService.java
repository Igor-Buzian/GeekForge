// src/main/java/com/example/spring/service/payment/PaypalService.java
package com.example.spring.service.payment;

import com.example.spring.entity.User;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaypalService implements PaymentService {

    private final APIContext apiContext;

    /**
     * Constructor for injecting APIContext using values from application.properties.
     * This is the preferred method if APIContext requires these parameters.
     *
     * @param clientId The PayPal client ID.
     * @param clientSecret The PayPal client secret.
     * @param mode The PayPal mode (e.g., "sandbox", "live").
     */
    public PaypalService(
            @Value("${paypal.client.id}") String clientId,
            @Value("${paypal.client.secret}") String clientSecret,
            @Value("${paypal.mode}") String mode) {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
        // Optional: Add logging for debugging
        // System.out.println("PayPalService initialized with mode: " + mode);
    }

    @Override
    public String getServiceType() {
        return "paypal";
    }

    @Override
    public Map<String, String> initiatePayment(
            BigDecimal totalAmount,
            String currency,
            String description,
            String cancelUrl,
            String successUrl) {

        Amount amount = new Amount();
        amount.setCurrency(currency);

        // Round BigDecimal to two decimal places and use toPlainString()
        BigDecimal roundedAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        amount.setTotal(roundedAmount.toPlainString());

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("PAYPAL");

        Payment payment = new Payment();
        payment.setIntent("sale"); // "sale" for immediate capture
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        try {
            Payment createdPayment = payment.create(apiContext);

            Map<String, String> response = new HashMap<>();
            for (Links links : createdPayment.getLinks()) {
                if (links.getRel().equals("approval_url")) {
                    response.put("approval_url", links.getHref());
                    return response;
                }
            }
            // If approval_url is not found, throw a RuntimeException
            throw new RuntimeException("Failed to get PayPal approval URL from created payment.");

        } catch (PayPalRESTException e) {
            e.printStackTrace();
            // Replace PaymentProcessingException with RuntimeException
            throw new RuntimeException("Error initiating PayPal payment: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            // Replace PaymentProcessingException with RuntimeException
            throw new RuntimeException("An unexpected error occurred during PayPal payment initiation: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> executePayment(String paymentId, String payerId, User user) {
        Payment payment = new Payment();
        payment.setId(paymentId); // Set the payment ID received from PayPal

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId); // Set the PayerID received from PayPal

        Map<String, String> result = new HashMap<>();

        try {
            // Execute the payment via the PayPal API
            Payment executedPayment = payment.execute(apiContext, paymentExecution);

            // Logging for debugging
            System.out.println("PayPal payment execution state: " + executedPayment.getState());
            System.out.println("PayPal executed payment details: " + executedPayment.toJSON());

            if ("approved".equals(executedPayment.getState())) {
                // Payment successfully approved by PayPal
                result.put("status", "approved");
                result.put("paymentId", executedPayment.getId());
                result.put("payerId", executedPayment.getPayer().getPayerInfo().getPayerId());

                // Get the transaction ID (Sale ID), which is the actual debit ID
                // This is very important information for future tracking and accounting in your system
                if (executedPayment.getTransactions() != null && !executedPayment.getTransactions().isEmpty()) {
                    Transaction transaction = executedPayment.getTransactions().get(0);
                    if (transaction.getRelatedResources() != null && !transaction.getRelatedResources().isEmpty()) {
                        RelatedResources relatedResources = transaction.getRelatedResources().get(0);
                        Sale sale = relatedResources.getSale(); // Get the Sale object
                        if (sale != null) {
                            result.put("transactionId", sale.getId()); // ID of the actual sales transaction
                            result.put("saleState", sale.getState()); // State of the sale (e.g., completed)
                        }
                    }
                }
                return result; // Return Map with successful payment details
            } else {
                // Payment was not approved by PayPal (e.g., 'failed', 'pending', 'canceled')
                result.put("status", executedPayment.getState());
                result.put("message", "PayPal payment was not approved. Current state: " + executedPayment.getState());
                // More details from executedPayment can be added if needed
                return result; // Return result with unsuccessful status
            }

        } catch (PayPalRESTException e) {
            // Error when calling PayPal API (e.g., incorrect credentials, network issues, invalid IDs)
            System.err.println("PayPalRESTException during executePayment: " + e.getMessage());
            e.printStackTrace(); // For detailed information in logs
            // Replace PaymentProcessingException with RuntimeException
            throw new RuntimeException("Error communicating with PayPal API during payment execution: " + e.getMessage(), e);
        } catch (Exception e) {
            // Any other unexpected error
            System.err.println("Unexpected error during PayPal payment execution: " + e.getMessage());
            e.printStackTrace();
            // Replace PaymentProcessingException with RuntimeException
            throw new RuntimeException("An unexpected error occurred during PayPal payment execution: " + e.getMessage(), e);
        }
    }
}