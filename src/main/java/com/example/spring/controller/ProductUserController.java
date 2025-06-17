package com.example.spring.controller;

import com.example.spring.dto.product.ProductFilterAdminDto;
import com.example.spring.dto.product.ProductFilterUserDto;
import com.example.spring.service.product.ProductUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1")
public class ProductUserController {
   private final ProductUserService productUserService;

    @GetMapping("/getAllProducts")
    public ResponseEntity<?> getFilteredAndPagedProducts(
            // @ModelAttribute позволяет Spring заполнять DTO из параметров запроса
            // (например, ?productName=test&minPrice=10&page=0&size=10)
            @ModelAttribute ProductFilterUserDto filterDto) {

        // Вызываем метод сервиса, который уже содержит логику фильтрации
        return productUserService.getFilteredAndPagedProducts(filterDto);

    }
}
