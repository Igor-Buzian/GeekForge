package com.example.spring.dto.category;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * DTO for representing category information in responses.
 * Contains data returned to the client after category retrieval.
 */
@Data
public class CategoryResponseDto {
    private Long id;

    @NotEmpty(message = "Categoty is not be null")
    private String name;
}