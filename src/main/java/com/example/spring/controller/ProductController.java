package com.example.spring.controller;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("product/v1/admin")
public class ProductController {
    private  final ProductService productService;
    @GetMapping("/create")
    ResponseEntity<?> createProduct(ProductCreateDto productCreateDto){
        return  productService.createProduct(productCreateDto);
    }
    @GetMapping("/update")
    ResponseEntity<?> updateProduct(ProductUpdateDto productUpdateDto){
        return productService.updateProduct(productUpdateDto);
    }

    @GetMapping("/delete")
    ResponseEntity<?> deleteProduct(String name){
        return productService.DeleteById(name);
    }

}



