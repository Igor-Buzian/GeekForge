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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final RepositoryService repositoryService;

    @Transactional
    public ResponseEntity<?> createCategory(CategoryCreateDto categoryCreateDto) {
        if (categoryRepository.existsByName(categoryCreateDto.getName()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("Error","Category with name: "+categoryCreateDto.getName()+" is exist!"));
        //   return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/category-form?error=categoryExist").build();
        Category category = new Category();//repositoryService.loadCategoryByName(categoryCreateDto.getName());
        category.setName(categoryCreateDto.getName());
        category.setSlug(categoryCreateDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        categoryRepository.save(category);
        // Возвращаем информацию о созданной категории в теле ответа
        CategoryResponseDto responseDto = new CategoryResponseDto();
        responseDto.setId((long) category.getId()); // Предполагаем, что ID CategoryResponseDto имеет тип Long
        responseDto.setName(category.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
      //  return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/category-form").build();
    }

    @Transactional
    public ResponseEntity<?> updateCategory(CategoryResponseDto categoryResponseDto) {

       Category category = categoryRepository.findById(Math.toIntExact(categoryResponseDto.getId()))
               .orElseThrow(() -> new IllegalAccessError("This ID is not exist!"));

        if(categoryRepository.existsByName(categoryResponseDto.getName())) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("Error", "Category name "+categoryResponseDto.getName()+" is exist!"));

        category.setName(categoryResponseDto.getName());
        category.setSlug(categoryResponseDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        categoryRepository.save(category);
        return  ResponseEntity.status(HttpStatus.CREATED).body(categoryResponseDto);
    }

    @Transactional
    public void deleteCategory(String name) {

        Category category = repositoryService.loadCategoryByName(name);
        categoryRepository.deleteById(Math.toIntExact(category.getId()));
    }

}
