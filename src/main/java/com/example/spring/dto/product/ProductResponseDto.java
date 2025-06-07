// src/main/java/com/example/spring/dto/product/ProductResponseDto.java
package com.example.spring.dto.product;

import com.example.spring.entity.Category;
import com.example.spring.entity.Product; // Импортируйте вашу сущность Product
import com.example.spring.service.firebase.FirebaseStorageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductResponseDto {
    private Long id; // Используйте Long для ID
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity; // Количество обычно Integer
    private String productImage;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Set<String> categoryNames; // Список только имен категорий

}