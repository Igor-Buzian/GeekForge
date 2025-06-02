package com.example.spring.controller;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("product/v1/admin")
public class ProductController {
    private  final ProductService productService;
    @PostMapping("/create")
    ResponseEntity<?> createProduct(ProductCreateDto productCreateDto){
        return  productService.createProduct(productCreateDto);
    }
    @PutMapping("/update")
    ResponseEntity<?> updateProduct(ProductUpdateDto productUpdateDto){
        return productService.updateProduct(productUpdateDto);
    }


    @DeleteMapping("/delete/{name}") // Используйте DELETE для удаления, имя в пути
    public ResponseEntity<?> deleteProduct(@PathVariable String name) { // Используем @PathVariable
        try {
            productService.deleteByName(name); // Переименовал метод в сервисе для ясности
            return ResponseEntity.ok("Product deleted successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting product: " + e.getMessage());
        }
    }
}



