package com.example.spring.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterUserDto {

    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String categoryNames; // Строка категорий, разделенных запятыми (например, "Electronics,Books")
    private String sortBy;
    private String sortDirection; // "asc" или "desc"
    private int page;
    private int size ;
}