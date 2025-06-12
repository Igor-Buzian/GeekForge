package com.example.spring.controller;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.product.ProductFilterDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("product/v1/admin")
public class ProductController {
    private  final ProductService productService;
    @PostMapping("/create")
    ResponseEntity<?> createProduct(ProductCreateDto productCreateDto, @RequestParam("imageFile") MultipartFile imageFile){
        try {
            return  productService.createProduct(productCreateDto,imageFile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/update")
    ResponseEntity<?> updateProduct(ProductUpdateDto productUpdateDto, @RequestParam(value = "imageFile", required = false) MultipartFile imageFile){
      try{
          return productService.updateProduct(productUpdateDto, imageFile);
      }catch (Exception e){
          System.err.println(e.getMessage());
        return  ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("Error: ",e.getMessage()));
      }
    }


    @DeleteMapping("/delete/{id}") // Используйте DELETE для удаления, имя в пути
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) { // Используем @PathVariable
        try {
            productService.deleteById(id); // Переименовал метод в сервисе для ясности
            return ResponseEntity.ok("Product deleted successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting product: " + e.getMessage());
        }
    }

    @GetMapping("/getAllProducts")
    public ResponseEntity<?> getAllProducts(@ModelAttribute ProductFilterDto filterDto){
      return  productService.getFilteredAndPagedProducts(filterDto);
    }
}



