package com.example.spring.repository;

import com.example.spring.dto.product.ProductResponseDto;
import com.example.spring.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String Name);
    Optional<Product> findById(Long id);
    List<Product> findByCategories_Name(String categoryName);

    boolean existsByName(String name);
    @Override
    boolean existsById(Long id);

    @Query("select distinct p from Product p join p.categories c where"+
            "(lower(CAST(p.name AS string)) like lower(concat('%', :productName, '%')) or :productName is null) and"+ // ИЗМЕНЕНО: AS string
            "(:minPrice is null or p.price >= :minPrice) and"+
            "(:maxPrice is null or p.price <= :maxPrice) and"+
            "(lower(CAST(c.name AS string)) like lower(concat('%', :categories, '%')) or :categories is null) and"+ // ИЗМЕНЕНО: AS string
            "(:updateBefore is null or p.updated<=:updateBefore) and"+
            "(:updateAfter is null or p.updated>=:updateAfter) and"+
            "(:createBefore is null or p.created<=:createBefore) and"+
            "(:createAfter is null or p.created>=:createAfter)"
    )
    Page<Product> findFilterProduct(@Param("productName") String productName,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("categories") String categories,
                                    @Param("updateBefore") LocalDateTime updateBefore,
                                    @Param("updateAfter") LocalDateTime updateAfter,
                                    @Param("createBefore") LocalDateTime createBefore,
                                    @Param("createAfter") LocalDateTime createAfter,
                                    Pageable pageable
    );
    /*    @Query("select distinct p from Product p join p.categories c where"+
            "(lower(CAST(p.name AS string)) like lower(concat('%', :productName, '%')) or :productName is null) and"+ // ИЗМЕНЕНО: AS string
            "(:minPrice is null or p.price >= :minPrice) and"+
            "(:maxPrice is null or p.price <= :maxPrice) and"+
            "(lower(CAST(c.name AS string)) like lower(concat('%', :categories, '%')) or :categories is null) and"+ // ИЗМЕНЕНО: AS string
            "(:updateBefore is null or p.updated<=:updateBefore) and"+
            "(:updateAfter is null or p.updated>=:updateAfter) and"+
            "(:createBefore is null or p.created<=:createBefore) and"+
            "(:createAfter is null or p.created>=:createAfter)"
    )
    Page<Product> findFilterProduct(@Param("productName") String productName,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("categories") String categories,
                                    @Param("updateBefore") LocalDateTime updateBefore,
                                    @Param("updateAfter") LocalDateTime updateAfter,
                                    @Param("createBefore") LocalDateTime createBefore,
                                    @Param("createAfter") LocalDateTime createAfter,
                                    Pageable pageable
    );*/

}
