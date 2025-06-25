package com.example.spring.service.product;

import com.example.spring.dto.search.ProductFilterUserDto;
import com.example.spring.dto.product.ProductResponseUserDto;
import com.example.spring.entity.Category;
import com.example.spring.entity.Product;
import com.example.spring.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductUserService {
    @Value("${list.products.pagination.default.page}")
    private int defaultPage;
    @Value("${list.products.pagination.default.size}")
    private int defaultSize;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true) // Транзакция только для чтения
    public ResponseEntity<?> getFilteredAndPagedProducts(
            ProductFilterUserDto filterDto
    ){
        // Установка направления сортировки
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (filterDto.getSortDirection() != null && !filterDto.getSortDirection().isEmpty()) {
            sortDirection = "desc".equalsIgnoreCase(filterDto.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        }

        // Определение поля для сортировки (по умолчанию "created")
        String sortByField = (filterDto.getSortBy() == null || filterDto.getSortBy().isEmpty()) ? "created" : filterDto.getSortBy();
        int pageToUse = (filterDto.getPage()<0 ? defaultPage : filterDto.getPage());

        Pageable pageable = PageRequest.of(
                pageToUse,
                defaultSize,
                Sort.by(sortDirection, sortByField)
        );

        // Получение отфильтрованных и пагинированных продуктов
        Page<Product> productPage = productRepository.findAll(ProductSpecificationUser.withFilter(filterDto), pageable);

        // Преобразование Page<Product> в Page<ProductResponseDto>
        Page<ProductResponseUserDto> productResponsePage = productPage.map(product -> {
            ProductResponseUserDto dto = new ProductResponseUserDto();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setDescription(product.getDescription());
            dto.setPrice(product.getPrice());
            dto.setQuantity(product.getQuantity());
            dto.setProductImage(product.getProductImage());
            dto.setInStock(product.getQuantity() != null && product.getQuantity()>0);
            // Проверка на null для категорий перед stream()
            dto.setCategoryNames(product.getCategories() != null ?
                    product.getCategories().stream()
                            .map(Category::getName) // Использование ссылки на метод для чистоты
                            .collect(Collectors.toSet()) :
                    Collections.emptySet()); // Возвращаем пустой Set, если категорий нет
            return dto;
        });

        return ResponseEntity.ok(productResponsePage);
    }
}
