package com.example.spring.repository.busket;

import com.example.spring.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Override
    Optional<CartItem> findById(Long id);

    boolean existsById(Long id);
}
