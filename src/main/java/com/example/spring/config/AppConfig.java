package com.example.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean // This annotation makes RestTemplate a Spring bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
