package com.example.spring.controller;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.product.ProductFilterAdminDto;
import com.example.spring.dto.product.ProductResponseDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.service.product.ProductAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("product/v1/admin")
public class ProductAdminController {
    private  final ProductAdminService productService;
    @PostMapping("/create")
    ResponseEntity<?> createProduct(ProductCreateDto productCreateDto, @RequestParam("imageFile") MultipartFile imageFile){
        try {
            return  productService.createProduct(productCreateDto,imageFile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/update")
    ResponseEntity<?> updateProduct( ProductUpdateDto productUpdateDto, @RequestParam(value = "imageFile", required = false) MultipartFile imageFile){
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
    public ResponseEntity<Page<ProductResponseDto>> getFilteredAndPagedProducts(
            // @ModelAttribute позволяет Spring заполнять DTO из параметров запроса
            // (например, ?productName=test&minPrice=10&page=0&size=10)
            ProductFilterAdminDto filterDto) {

        // Вызываем метод сервиса, который уже содержит логику фильтрации
        ResponseEntity<?> response = productService.getFilteredAndPagedProducts(filterDto);

        // Убедитесь, что сервис возвращает ResponseEntity<Page<ProductResponseDto>>
        // или перехватите и преобразуйте, если он возвращает ResponseEntity<?>
        // Если getFilteredAndPagedProducts уже возвращает Page<ProductResponseDto> напрямую,
        // то метод контроллера может быть проще.
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() instanceof Page) {
            @SuppressWarnings("unchecked") // Подавляем предупреждение о небезопасном приведении типов
            Page<ProductResponseDto> productPage = (Page<ProductResponseDto>) response.getBody();
            return ResponseEntity.ok(productPage);
        } else {
            // Обработка ошибок или нежелательных ответов от сервиса
            // Например, возвращаем пустую страницу или ошибку
            return ResponseEntity.status(response.getStatusCode()).build();
        }
    }
}



