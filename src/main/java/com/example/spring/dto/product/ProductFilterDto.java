package com.example.spring.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDto {
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String categoryNames; // Строка категорий, разделенных запятыми
    private String sortBy;
    private String sortDirection;
    private LocalDateTime updatedBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime createdAfter;
    private int page;
    private int size =10;
}
