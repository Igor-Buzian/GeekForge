package com.example.spring.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for representing product information in responses.
 * Contains data returned to the client after product retrieval.
 */
@Data
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;
    private Set<String> categoryNames; // Names of associated categories
}