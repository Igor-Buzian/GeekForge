// com.example.spring.dto.cart.CartItemRequestDto.java
package com.example.spring.dto.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {
    private Long productId;  // The ID of the product to add or update
    private Integer quantity; // The quantity of the product
}