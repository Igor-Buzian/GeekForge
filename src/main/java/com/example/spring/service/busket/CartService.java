// com.example.spring.service.busket.CartService.java
package com.example.spring.service.busket;

import com.example.spring.dto.cart.CartFilterDto;
import com.example.spring.dto.cart.CartItemDto;
import com.example.spring.dto.cart.CartItemRequestDto;
import com.example.spring.dto.cart.CartResponseDto;
import com.example.spring.entity.Cart;
import com.example.spring.entity.CartItem;
import com.example.spring.entity.Product;
import com.example.spring.entity.User;
import com.example.spring.repository.ProductRepository;
import com.example.spring.repository.UserRepository;
import com.example.spring.repository.busket.CartItemRepository;
import com.example.spring.repository.busket.CartRepository;
import com.example.spring.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    @Value("${list.products.pagination.default.page}")
    private int defaultPage;
    @Value("${list.products.pagination.default.size}")
    private int defaultSize;

    @Transactional(readOnly = true)
    public CartResponseDto getUserCart(User user) { // <<< ЭТОТ МЕТОД НУЖЕН ВАШЕМУ КОНТРОЛЛЕРУ >>>


        return cartRepository.findByUser(user)
                .map(this::mapCartToCartResponseDto)
                .orElseGet(() -> new CartResponseDto( null, null, Collections.emptyList(), BigDecimal.ZERO, 0));
    }

    /**
     * Retrieves the cart items for a given user, with optional filtering by product name and pagination.
     * Initially, it returns all items in the cart with pagination.
     *
     * @param filterDto DTO containing product name for search, page number, size, and sort options.
     * @return A Page of CartItemDto representing the items in the user's cart.
     */
    @Transactional(readOnly = true)
    public Page<CartItemDto> getUserCartItems(User user, CartFilterDto filterDto) {

        Cart userCart = cartRepository.findByUser(user)
                .orElse(null);

        if (userCart == null || userCart.getCartItems().isEmpty()) {
            // If cart is not found or empty, return an empty page.
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(defaultPage, defaultSize), 0);
        }

        // Important: Ensure cartItems are eagerly loaded or fetch them explicitly
        // For @OneToMany relation, if FetchType.LAZY, you might need to initialize them:
        // Hibernate.initialize(userCart.getCartItems());
        // Or change FetchType to EAGER in Cart entity for 'cartItems' field.
        // For now, assuming they are available due to Transactional(readOnly = true) and typical usage.
        List<CartItem> allCartItems = userCart.getCartItems().stream().toList();

        // 1. Optional Filtering by product name
        if (filterDto.getProductName() != null && !filterDto.getProductName().trim().isEmpty()) {
            String searchTerm = filterDto.getProductName().trim().toLowerCase();
            allCartItems = allCartItems.stream()
                    .filter(item -> item.getProduct().getName().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }


        // 3. Manual Pagination (since we're filtering/sorting in memory)
        int start = (int) PageRequest.of(filterDto.getPage(), defaultSize).getOffset();
        int end = Math.min((start + defaultSize), allCartItems.size());

        // Handle cases where start is out of bounds (e.g., page number too high for available items)
        if (start > allCartItems.size()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(filterDto.getPage(), defaultSize), allCartItems.size());
        }

        List<CartItem> pagedItems = allCartItems.subList(start, end);

        List<CartItemDto> itemDtos = pagedItems.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getProductImage(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(itemDtos, PageRequest.of(filterDto.getPage(), defaultSize), allCartItems.size());
    }

    // --- All other methods (addProductToCart, removeProductFromCart, updateCartItemQuantity, mapCartToCartResponseDto) remain unchanged from previous versions ---

    // (Previous methods would be here)

    @Transactional
    public CartResponseDto addProductToCart(User user, CartItemRequestDto requestDto) {

        Product product = repositoryService.loadProductById(requestDto.getProductId());

        if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a positive number.");
        }

        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCreated(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });

        Optional<CartItem> existingCartItem = cartItemRepository.findByCartAndProduct(userCart, product);

        CartItem cartItem;
        Integer quantityToAdd = requestDto.getQuantity();

        if (existingCartItem.isPresent()) {
            cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + quantityToAdd;
            if (product.getQuantity() != null && product.getQuantity() < newQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Not enough product '%s' in stock. Available: %d, trying to add: %d",
                                product.getName(), product.getQuantity(), newQuantity - cartItem.getQuantity()));
            }
            cartItem.setQuantity(newQuantity);
        } else {
            if (product.getQuantity() != null && product.getQuantity() < quantityToAdd) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Not enough product '%s' in stock. Available: %d, trying to add: %d",
                                product.getName(), product.getQuantity(), quantityToAdd));
            }
            cartItem = new CartItem();
            cartItem.setCart(userCart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantityToAdd);
        }

        cartItemRepository.save(cartItem);
        cartRepository.save(userCart);

        return mapCartToCartResponseDto(userCart); // Возвращаем полную корзину после добавления
    }

    @Transactional
    public CartResponseDto removeProductFromCart(User user, Long productId) {

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not in cart."));


        // Удаляем CartItem ИЗ КОЛЛЕКЦИИ cartItems в объекте userCart.
        // Благодаря orphanRemoval = true, Hibernate/JPA автоматически удалит этот CartItem
        // из базы данных при завершении транзакции.
        userCart.getCartItems().remove(cartItem);

        // НЕ нужно вызывать cartItemRepository.delete(cartItem); вручную,
        // так как orphanRemoval=true сделает это автоматически.
        // Вызов cartRepository.save(userCart); также не строго необходим,
        // так как userCart уже находится в контексте персистентности,
        // и изменение его коллекции будет автоматически обнаружено.
        // Однако, его можно оставить, если это помогает ясности или в других случаях.
        // cartRepository.save(userCart); // Можно убрать или оставить по желанию.

        // Важно: если вы удалили элемент из коллекции,
        // userCart теперь содержит актуальный список CartItem.
        // mapCartToCartResponseDto должен брать данные из этого объекта userCart.
        return mapCartToCartResponseDto(userCart);
    }

    @Transactional
    public CartResponseDto updateCartItemQuantity(User user, CartItemRequestDto requestDto) {

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + requestDto.getProductId()));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not in cart."));

        Integer newQuantity = requestDto.getQuantity();

        if (newQuantity == null || newQuantity <= 0) {
            return removeProductFromCart(user, requestDto.getProductId());
        }

        if (product.getQuantity() != null && product.getQuantity() < newQuantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Not enough product '%s' in stock. Available: %d, trying to set: %d",
                            product.getName(), product.getQuantity(), newQuantity));
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        cartRepository.save(userCart);

        return mapCartToCartResponseDto(userCart);
    }

    private CartResponseDto mapCartToCartResponseDto(Cart cart) {
        List<CartItem> cartItems = cart.getCartItems() != null ? cart.getCartItems().stream().toList() : Collections.emptyList();

        List<CartItemDto> itemDtos = cartItems.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getProductImage(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .collect(Collectors.toList());

        BigDecimal totalCost = itemDtos.stream()
                .map(CartItemDto::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalQuantity = itemDtos.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        return new CartResponseDto(
                cart.getId(),
                cart.getUser().getId(),
                itemDtos,
                totalCost,
                totalQuantity
        );
    }
}