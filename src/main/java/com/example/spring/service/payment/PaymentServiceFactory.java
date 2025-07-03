// src/main/java/com/example/spring/service/payment/PaymentServiceFactory.java
package com.example.spring.service.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentServiceFactory {

    private final Map<String, PaymentService> paymentServices;

    /**
     * Constructs a PaymentServiceFactory, injecting all beans that implement PaymentService.
     * These services are then stored in a map, keyed by their service type.
     *
     * @param paymentServiceList A list of all PaymentService beans automatically injected by Spring.
     */
    public PaymentServiceFactory(List<PaymentService> paymentServiceList) {
        this.paymentServices = paymentServiceList.stream()
                .collect(Collectors.toMap(PaymentService::getServiceType, Function.identity()));
    }

    /**
     * Retrieves a PaymentService implementation based on the provided service type.
     * The service type is converted to lowercase for flexible matching.
     *
     * @param serviceType The unique identifier for the desired payment service (e.g., "paypal", "stripe").
     * @return The PaymentService implementation corresponding to the given type.
     * @throws IllegalArgumentException If no payment service is found for the specified type.
     */
    public PaymentService getPaymentService(String serviceType) {
        PaymentService service = paymentServices.get(serviceType.toLowerCase()); // Convert to lowercase for flexibility
        if (service == null) {
            throw new IllegalArgumentException("Payment service not found for type: " + serviceType);
        }
        return service;
    }
}