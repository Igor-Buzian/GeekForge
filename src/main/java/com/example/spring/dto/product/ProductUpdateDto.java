package com.example.spring.dto.product;

import com.example.spring.entity.Category;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
/**
 * DTO for updating an existing product.
 * Contains fields that can be modified for a product, with validation.
 */
@Data
public class ProductUpdateDto {

    private Long id;

    @Size(max=255, message = "Name is more than 255 letters")
    private String newName;

    @Size(max=255, message = "Description is more than 255 letters")
    private  String description;

    @Min(value = 0,  message = "Price can't be less than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity can't be less than 0")
    private Integer quantity;

    private Set<String> categoryNames;
}
