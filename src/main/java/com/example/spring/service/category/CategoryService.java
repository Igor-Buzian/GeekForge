package com.example.spring.service.category;

import com.example.spring.dto.category.CategoryCreateDto;
import com.example.spring.dto.category.CategoryResponseDto;
import com.example.spring.entity.Category;
import com.example.spring.repository.CategoryRepository;
import com.example.spring.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private  final RepositoryService repositoryService;


    ResponseEntity<?> createCategory(CategoryCreateDto categoryCreateDto){
        if(categoryRepository.existsByName(categoryCreateDto.getName())) return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/category-form?error=categoryExist").build();
        Category category = repositoryService.loadCategoryByName(categoryCreateDto.getName());
        category.setName(categoryCreateDto.getName());
        category.setSlug(categoryCreateDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        categoryRepository.save(category);

        return  ResponseEntity.status(HttpStatus.CREATED).header("Location","/category-form").build();
    }

    ResponseEntity<?> updateCategory(CategoryResponseDto categoryResponseDto){
        if(!categoryRepository.existsByName(categoryResponseDto.getName())) return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/category-form?error=categoryNotExist").build();

        Category category = repositoryService.loadCategoryByName(categoryResponseDto.getName());
        category.setName(categoryResponseDto.getName());
        category.setSlug(categoryResponseDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        return  ResponseEntity.status(HttpStatus.CREATED).header("Location","/category-updated").build();
    }

    ResponseEntity<?> deleteCategory(String name){

       Category category = repositoryService.loadCategoryByName(name);
        categoryRepository.save(category);
        
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location","created-delete").build();
    }

}
