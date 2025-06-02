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

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final RepositoryService repositoryService;

    @Transactional
    public ResponseEntity<?> createCategory(CategoryCreateDto categoryCreateDto) {
        if (categoryRepository.existsByName(categoryCreateDto.getName()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Category with name: "+categoryCreateDto.getName()+" is exist!");
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
    @Transactional // Добавлен @Transactional для атомарности
    public ResponseEntity<?> updateCategory(CategoryResponseDto categoryResponseDto) {
        // Находим категорию по ID для обновления, так как имя может измениться
        Category category = categoryRepository.findById(Math.toIntExact(categoryResponseDto.getId())) // Преобразуем Long ID в int
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена с ID: " + categoryResponseDto.getId()));

        // Проверяем, существует ли новое имя для *другой* категории
        if (categoryRepository.existsByName(categoryResponseDto.getName()) &&
                !category.getName().equalsIgnoreCase(categoryResponseDto.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Категория с именем '" + categoryResponseDto.getName() + "' уже существует для другой категории.");
        }

        category.setName(categoryResponseDto.getName());
        category.setSlug(categoryResponseDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        categoryRepository.save(category);

        // Возвращаем информацию об обновленной категории
        CategoryResponseDto responseDto = new CategoryResponseDto();
        responseDto.setId((long) category.getId());
        responseDto.setName(category.getName());
        return ResponseEntity.ok(responseDto); // Используйте OK (200) для успешных обновлений
    }

    @Transactional // Добавлен @Transactional для атомарности
    public void deleteCategory(String name) { // Изменен тип возвращаемого значения на void; исключения обрабатывают ошибки
        Category category = repositoryService.loadCategoryByName(name); // Предполагается, что это выбрасывает исключение, если не найдено
        categoryRepository.delete(category);
    }
}
   /* public ResponseEntity<?> updateCategory(CategoryResponseDto categoryResponseDto) {
        if (!categoryRepository.existsByName(categoryResponseDto.getName()))
            return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/category-form?error=categoryNotExist").build();

        Category category = repositoryService.loadCategoryByName(categoryResponseDto.getName());
        category.setName(categoryResponseDto.getName());
        category.setSlug(categoryResponseDto.getName().trim().toLowerCase().replaceAll("\\s+", "-"));

        return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/category-updated").build();
    }

    public ResponseEntity<?> deleteCategory(String name) {

        Category category = repositoryService.loadCategoryByName(name);
        categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "created-delete").build();
    }

}*/
