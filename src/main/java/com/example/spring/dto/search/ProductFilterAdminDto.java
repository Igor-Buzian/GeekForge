package com.example.spring.dto.search;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterAdminDto {
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