package com.example.spring.service.product;

import com.example.spring.dto.product.ProductCreateDto;
import com.example.spring.dto.search.ProductFilterAdminDto;
import com.example.spring.dto.product.ProductResponseDto;
import com.example.spring.dto.product.ProductUpdateDto;
import com.example.spring.entity.Category;
import com.example.spring.entity.Product;
import com.example.spring.repository.CategoryRepository;
import com.example.spring.repository.ProductRepository;
import com.example.spring.service.RepositoryService;
import com.example.spring.service.firebase.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.io.Files.getFileExtension;

@Service
@RequiredArgsConstructor
public class ProductAdminService {
    @Value("${list.products.pagination.default.page}")
    private int defaultPage;
    @Value("${list.products.pagination.default.size}")
    private  int defaultSize;


    private final RepositoryService repositoryService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
   private final FirebaseStorageService firebaseStorageService;

    @Transactional
    public ResponseEntity<?> createProduct(ProductCreateDto productCreateDto, MultipartFile imageFile) {
        if (productRepository.existsByName(productCreateDto.getName())) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER).body(Map.of("Error: ", "This product is Exist!"));
        }
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // Генерируем имя файла на основе имени продукта
                // Удаляем пробелы и спецсимволы, делаем lowercase
                String cleanProductName = productCreateDto.getName().replaceAll("[^a-zA-Z0-9-]", "").toLowerCase();
                String fileExtension = getFileExtension(imageFile.getOriginalFilename());
                String fileNameInStorage = cleanProductName + "-" + System.currentTimeMillis() + fileExtension; // Добавляем timestamp для уникальности
                // Загружаем изображение в Firebase Storage в папку "product-images/"
                imageUrl = firebaseStorageService.uploadFile(imageFile, "product-images/" + fileNameInStorage);
            } catch (IOException e) {
                System.err.println("Error uploading image to Firebase Storage: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload image: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("An unexpected error occurred during image upload: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred during image upload: " + e.getMessage());
            }
        }

        Product product = new Product();
        product.setCreated(LocalDateTime.now());
        product.setUpdated(LocalDateTime.now());
        product.setName(productCreateDto.getName());
        product.setDescription(productCreateDto.getDescription());
        product.setPrice(productCreateDto.getPrice());
        product.setQuantity(productCreateDto.getQuantity());

        Set<Category> categories = productCreateDto.getCategoryNames().stream()
                .map(categoryName -> categoryRepository.findByName(categoryName.toLowerCase())
                        .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryName)))
                .collect(Collectors.toSet());

        product.setCategories(categories);
        product.setProductImage(imageUrl);
        productRepository.save(product);
        return  ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Message: ", "This product was success created!"));
    }

    @Transactional
    public ResponseEntity<?> updateProduct(ProductUpdateDto productUpdateDto, MultipartFile imageFile) {
        if (!productRepository.existsById(productUpdateDto.getId()))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error: ", "This product is not exist!"));

        Product product = repositoryService.loadProductById(productUpdateDto.getId());

        if(productUpdateDto.getNewName()!=null) product.setName(productUpdateDto.getNewName());
        if(productUpdateDto.getQuantity()!=null) product.setQuantity(productUpdateDto.getQuantity());

        if(productUpdateDto.getPrice()!= null) product.setPrice(productUpdateDto.getPrice());

        if(productUpdateDto.getDescription()!=null) product.setDescription(productUpdateDto.getDescription());

        product.setUpdated(LocalDateTime.now());

        if(productUpdateDto.getCategoryNames()!=null && !productUpdateDto.getCategoryNames().isEmpty()){
            Set<Category> categories = productUpdateDto.getCategoryNames().stream()
                    .map(category -> categoryRepository.findByName(category.toLowerCase()).orElseThrow(() -> new NoSuchElementException("Category not found: "+category)))
                    .collect(Collectors.toSet());
            product.setCategories(categories);
        }

        if(imageFile!=null && !imageFile.isEmpty()){
            try {

                if(product.getProductImage()!=null && !product.getProductImage().isEmpty()){
                  String url =  firebaseStorageService.extractFilePathFromFirebaseUrl(product.getProductImage());
                    firebaseStorageService.deleteFile(url);
                }

                String productName = product.getName().replaceAll("[^a-zA-Z0-9-]", "").toLowerCase();
                String fileExtention = getFileExtension(imageFile.getOriginalFilename());
                String  fileNameInStorage = productName + "-" + System.currentTimeMillis() + fileExtention;
                String imageUrl = firebaseStorageService.uploadFile(imageFile, "product-images/"+fileNameInStorage);
                product.setProductImage(imageUrl);

            } catch (Exception e) {
                System.err.println("Error: "+e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("Error: ",e.getMessage()));
            }
        }

        productRepository.save(product);

        return  ResponseEntity.status(HttpStatus.CREATED).body(Map.of("Message: ", "This product was success updated!"));
    }

    @Transactional
    public ResponseEntity<?> deleteById(Long id) {
        Product product = repositoryService.loadProductById(id);
        productRepository.deleteById(product.getId());
        if(product.getProductImage()!=null && !product.getProductImage().isEmpty()){
            String url = firebaseStorageService.extractFilePathFromFirebaseUrl(product.getProductImage());
            firebaseStorageService.deleteFile(url);
        }
        return ResponseEntity.ok("Successful delete product!");
    }

    @Transactional(readOnly = true) // Транзакция только для чтения
    public ResponseEntity<?> getFilteredAndPagedProducts(
            ProductFilterAdminDto filterDto
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
        Page<Product> productPage = productRepository.findAll(ProductSpecificationAdmin.withFilters(filterDto), pageable);

        // Преобразование Page<Product> в Page<ProductResponseDto>
        Page<ProductResponseDto> productResponsePage = productPage.map(product -> {
            ProductResponseDto dto = new ProductResponseDto();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setDescription(product.getDescription());
            dto.setPrice(product.getPrice());
            dto.setQuantity(product.getQuantity());
            dto.setProductImage(product.getProductImage());
            dto.setCreated(product.getCreated());
            dto.setUpdated(product.getUpdated());
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

