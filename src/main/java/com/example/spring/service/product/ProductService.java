package com.example.spring.service.product;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.entity.Category;
import com.example.spring.entity.Product;
import com.example.spring.repository.CategoryRepository;
import com.example.spring.repository.ProductRepository;
import com.example.spring.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final RepositoryService repositoryService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    @Transactional
    public ResponseEntity<?> createProduct(ProductCreateDto productCreateDto) {
        if (productRepository.existsByName(productCreateDto.getName())) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/create-product?error=productExist").build();
        }

        Product product = new Product();
        product.setCreated(LocalDateTime.now());
        product.setUpdated(LocalDateTime.now());
        product.setName(productCreateDto.getName());
        product.setDescription(productCreateDto.getDescription());
        product.setPrice(productCreateDto.getPrice());
        product.setQuantity(productCreateDto.getQuantity());
        Set<Category> categories = productCreateDto.getCategoryNames().stream()
                .map(categoryName -> categoryRepository.findByName(categoryName)
                        .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryName)))
                .collect(Collectors.toSet());
        product.setCategories(categories);
        //  product.setProductImage(product.getProductImage());
        productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/success").build();
    }
    @Transactional
    public ResponseEntity<?> updateProduct(ProductUpdateDto productUpdateDto) {
        if (!productRepository.existsByName(productUpdateDto.getName()))
            return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/update-product?error=productNotExist").build();

        Product product = repositoryService.loadProductByName(productUpdateDto.getName());
        product.setName(productUpdateDto.getName());
        product.setPrice(productUpdateDto.getPrice());
        product.setDescription(productUpdateDto.getDescription());
        product.setQuantity(productUpdateDto.getQuantity());
        Set<Category> categories = productUpdateDto.getCategoryNames().stream()
                .map(category -> categoryRepository.findByName(category).orElseThrow(() -> new NoSuchElementException("Category not found: "+category)))
                        .collect(Collectors.toSet());
        product.setCategories(categories);

        productRepository.save(product);

        return ResponseEntity.status(HttpStatus.CREATED).header("Location", "/success").build();
    }
    @Transactional
    public ResponseEntity<?> deleteByName(String name) {
        Product product = repositoryService.loadProductByName(name);
        productRepository.deleteById(product.getId());
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Location", "/product-delete").build();
    }

}
