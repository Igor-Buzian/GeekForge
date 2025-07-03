// src/main/java/com/example/spring/service/payment/StripeService.java
package com.example.spring.service.payment;

import com.example.spring.entity.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService implements PaymentService {

    // Stripe API Key injected from application.properties
    public StripeService(@Value("${stripe.api.key}") String secretKey) {
        Stripe.apiKey = secretKey; // Set the Stripe secret key globally for the SDK
        // Optional: Log for debugging
        // System.out.println("StripeService initialized with API Key.");
    }

    @Override
    public String getServiceType() {
        return "stripe";
    }

    @Override
    public Map<String, String> initiatePayment(
            BigDecimal totalAmount,
            String currency,
            String description,
            String cancelUrl, // Not directly used by Stripe for initial PI creation, but good to keep for interface consistency
            String successUrl) { // Not directly used by Stripe for initial PI creation

        try {
            // Stripe amounts are in cents/smallest currency unit
            Long amountInCents = totalAmount.multiply(new BigDecimal("100")).longValueExact();

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountInCents)
                            .setCurrency(currency)
                            .setDescription(description)
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            )
                            // You can add more parameters here for customer, metadata, etc.
                            .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Map<String, String> response = new HashMap<>();
            // For Stripe, we typically return the client_secret to the frontend.
            // The frontend then uses this secret with Stripe.js to confirm the payment.
            response.put("client_secret", paymentIntent.getClientSecret());
            // You might also return the PaymentIntent ID for your records
            response.put("payment_intent_id", paymentIntent.getId());

            return response;

        } catch (StripeException e) {
            e.printStackTrace();
            throw new RuntimeException("Error initiating Stripe payment: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("An unexpected error occurred during Stripe payment initiation: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> executePayment(String paymentId, String payerId, User user) {
        // For Stripe, 'paymentId' usually refers to the PaymentIntent ID.
        // 'payerId' is often not directly used in the same way as PayPal's PayerID
        // because confirmation is often done client-side or implicitly by the PaymentIntent.
        // However, we can use the paymentId to retrieve the PaymentIntent and verify its status.

        Map<String, String> result = new HashMap<>();

        try {
            // Retrieve the PaymentIntent using its ID
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

            System.out.println("Stripe PaymentIntent status: " + paymentIntent.getStatus());
            System.out.println("Stripe PaymentIntent details: " + paymentIntent.toJson());

            // Check if the payment intent was successfully processed
            // 'succeeded' means the payment was captured
            // 'requires_capture' means the payment was authorized and needs explicit capture if intent was 'capture'
            if ("succeeded".equals(paymentIntent.getStatus()) || "requires_capture".equals(paymentIntent.getStatus())) {
                result.put("status", "approved"); // Map Stripe status to a generic 'approved'
                result.put("paymentId", paymentIntent.getId()); // The PaymentIntent ID
                result.put("transactionId", paymentIntent.getId()); // In Stripe, PaymentIntent ID often serves as the primary transaction ID for tracking
                result.put("currency", paymentIntent.getCurrency());
                result.put("amount", paymentIntent.getAmount().toString());
                // If you need the charge ID (actual debit), you might retrieve it from latest_charge
                if (paymentIntent.getLatestChargeObject() != null) {
                    result.put("chargeId", paymentIntent.getLatestChargeObject().getId());
                }

                return result;
            } else {
                result.put("status", paymentIntent.getStatus()); // Return the actual Stripe status
                result.put("message", "Stripe payment was not successful. Current status: " + paymentIntent.getStatus());
                return result;
            }

        } catch (StripeException e) {
            System.err.println("StripeException during executePayment: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error communicating with Stripe API during payment execution: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error during Stripe payment execution: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("An unexpected error occurred during Stripe payment execution: " + e.getMessage(), e);
        }
    }
}