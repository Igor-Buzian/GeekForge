package com.example.spring.dto.category;

import lombok.Data;

import javax.validation.constraints.NotEmpty;


/**
 * DTO for creating a new product category.
 * Contains the name of the new category with validation.
 */
@Data
public class CategoryCreateDto {

    @NotEmpty(message = "Categoty is not be null")
    private String name;
}
