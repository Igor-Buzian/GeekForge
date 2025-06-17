package com.example.spring.dto.product;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class ProductResponseUserDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String productImage;
    private Set<String> categoryNames; // Названия категорий
    private boolean InStock;
    }

