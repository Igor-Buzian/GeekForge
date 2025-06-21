package com.example.spring.repository.busket;

import com.example.spring.entity.Cart;
import com.example.spring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository  extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user); // Добавьте этот метод, если его нет


    boolean existsByUser(User user);
}
