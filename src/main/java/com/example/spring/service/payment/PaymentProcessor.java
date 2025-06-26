package com.example.spring.service.payment;

import java.math.BigDecimal;

public interface PaymentProcessor {
    String processPayment(BigDecimal amount, String currency, String paymentId, String customerEmail) throws PaymentException;
    PaymentGatewayType getType();
}