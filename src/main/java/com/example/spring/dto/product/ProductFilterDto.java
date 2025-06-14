package com.example.spring.dto.product;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private LocalDate updatedBefore;
    private LocalDate updatedAfter;
    private LocalDate createdBefore;
    private LocalDate createdAfter;
    private int page;
    private int size ;
}