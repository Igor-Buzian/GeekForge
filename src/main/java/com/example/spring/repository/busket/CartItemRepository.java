package com.example.spring.repository.busket;

import com.example.spring.entity.Cart;
import com.example.spring.entity.CartItem;
import com.example.spring.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product); // Добавьте этот метод, если его нет

    boolean existsById(Long id);
}
