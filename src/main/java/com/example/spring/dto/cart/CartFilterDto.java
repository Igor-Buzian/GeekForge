// com.example.spring.dto.cart.CartFilterDto.java
package com.example.spring.dto.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartFilterDto {
    private String productName; // Поле для поиска по части названия продукта в корзине (опционально)
    private  Integer quantity;
    private int page = 0;       // Номер страницы (по умолчанию 0)
    private String sortBy = "id"; // Поле для сортировки (например, "id", "productName", "quantity", "itemTotal")
    private String sortDirection = "asc"; // Направление сортировки ("asc" для возрастания, "desc" для убывания)
}