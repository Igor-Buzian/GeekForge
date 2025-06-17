
package com.example.spring.controller;

import com.example.spring.dto.category.CategoryCreateDto;
import com.example.spring.dto.category.CategoryResponseDto;
import com.example.spring.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/category/v1/admin")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping("/create")
    ResponseEntity<?> createCategory(@RequestBody CategoryCreateDto categoryCreateDto) {
        return categoryService.createCategory(categoryCreateDto);
    }

    @PutMapping("/update")
    ResponseEntity<?> updateCategory(@RequestBody CategoryResponseDto categoryResponseDto) {
        return categoryService.updateCategory(categoryResponseDto);
    }


    @DeleteMapping("/delete/{name}")
    public ResponseEntity<?> deleteCategory(@PathVariable String name) {
        try {
            categoryService.deleteCategory(name);
            // Возвращаем JSON-объект с сообщением об успехе
            return ResponseEntity.ok().body(Map.of("message", "Category deleted successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error deleting category: " + e.getMessage()));
        }
    }
}



