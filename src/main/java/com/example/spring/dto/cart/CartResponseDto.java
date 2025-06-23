// com.example.spring.dto.cart.CartResponseDto.java
package com.example.spring.dto.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // Оставьте этот, он генерирует конструктор со всеми 6 полями

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Этот конструктор для всех 6 полей
public class CartResponseDto {
    private Long id;
    private Long userId;
    private List<CartItemDto> cartItems;
    private BigDecimal totalCost;
    private Integer totalQuantity;
    private String errorMessage; // Это поле теперь должно быть

    // --- ДОБАВЬТЕ ЭТОТ КОНСТРУКТОР ---
    // Этот конструктор будет использоваться для успешных ответов (без errorMessage)
    public CartResponseDto(Long id, Long userId, List<CartItemDto> cartItems, BigDecimal totalCost, Integer totalQuantity) {
        this.id = id;
        this.userId = userId;
        this.cartItems = cartItems;
        this.totalCost = totalCost;
        this.totalQuantity = totalQuantity;
        this.errorMessage = null; // По умолчанию null для успешных ответов
    }
}