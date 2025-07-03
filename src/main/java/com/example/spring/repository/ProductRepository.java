package com.example.spring.repository;

import com.example.spring.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String Name);
    Optional<Product> findById(Long id);
    List<Product> findByCategories_Name(String categoryName);

    boolean existsByName(String name);
    @Override
    boolean existsById(Long id);

}
