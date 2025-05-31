package com.example.spring.dto.product;

import com.example.spring.entity.Category;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Set;
/**
 * DTO for creating a new product.
 * Contains fields required for product creation with validation constraints.
 */
@Data
public class ProductCreateDto {

    @NotBlank(message = "Name is empty")
    private String name;

    @NotBlank(message = "Description is empty")
    private  String description;


    @Min(value = 0, message = "Price can't be less than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity can't be less than 0")
    private Integer  quantity;

    @NotBlank(message = "Image for product ought to be")
    private String productImage;

    private Set<String> categoryNames;
}
