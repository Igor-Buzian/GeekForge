// com.example.spring.dto.cart.CartResponseDto.java
package com.example.spring.dto.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private Long id;                  // ID of the cart itself
    private Long userId;              // ID of the user this cart belongs to
    private List<CartItemDto> cartItems; // List of all individual items in the cart
    private BigDecimal totalCost;     // The sum of itemTotal for all cart items
    private Integer totalQuantity;    // The sum of quantities for all cart items
}