package com.example.spring.dto.product;

import com.example.spring.entity.Category;
import lombok.Data;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Set;
/**
 * DTO for updating an existing product.
 * Contains fields that can be modified for a product, with validation.
 */
@Data
public class ProductUpdateDto {

    private String name;

    private  String description;

    @Min(value = 0,  message = "Price can't be less than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity can't be less than 0")
    private Integer quantity;

    private Set<String> categoryNames;
}
