// com.example.spring.dto.cart.CartItemDto.java
package com.example.spring.dto.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;              // ID of the cart item itself (not the product ID)
    private Long productId;       // ID of the product
    private String productName;   // Name of the product
    private String productImage;  // URL or path to the product image
    private BigDecimal productPrice; // Price of one unit of the product
    private Integer quantity;     // Quantity of this product in the cart
    private BigDecimal itemTotal; // Total cost for this specific cart item (productPrice * quantity)
}