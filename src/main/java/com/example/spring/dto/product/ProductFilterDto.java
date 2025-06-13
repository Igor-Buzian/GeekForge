package com.example.spring.dto.product;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // Убедитесь, что List используется, если хотите передавать несколько значений через API

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDto {
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String categoryNames; // Строка категорий, разделенных запятыми (например, "Electronics,Books")
    private String sortBy;
    private String sortDirection; // "asc" или "desc"
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime updatedBefore;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime updatedAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime createdBefore;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime createdAfter;
    private int page = 0; // Значение по умолчанию
    private int size = 10; // Значение по умолчанию
}