package com.example.spring.dto.search;

import lombok.Data;

/**
 * DTO for filtering and searching products.
 * Allows specifying criteria such as product name (partial match), category,
 * price range, and stock availability.
 */
@Data
public class ProductFilterDto {

    private String name;
    private String categoryName;
    private int minValue;
    private int maxValue;
    private boolean inStock;

}
