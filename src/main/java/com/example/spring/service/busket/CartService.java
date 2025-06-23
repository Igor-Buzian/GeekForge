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
import org.springframework.data.domain.Pageable; // Not used in this snippet, can remove if not needed
import org.springframework.data.domain.Sort; // Not used in this snippet, can remove if not needed
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
    public CartResponseDto getUserCart(User user) {
        return cartRepository.findByUser(user)
                .map(this::mapCartToCartResponseDto)
                .orElseGet(() -> new CartResponseDto( null, null, Collections.emptyList(), BigDecimal.ZERO, 0, null)); // Added null for errorMessage
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
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(defaultPage, defaultSize), 0);
        }

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

        if (start > allCartItems.size()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(filterDto.getPage(), defaultSize), allCartItems.size());
        }

        List<CartItem> pagedItems = allCartItems.subList(start, end);

        List<CartItemDto> itemDtos = pagedItems.stream()
                .map(item -> {
                    CartItemDto dto = new CartItemDto(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getProductImage(),
                            item.getProduct().getPrice(),
                            item.getQuantity(),
                            item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                            item.isSelected()
                    );
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(itemDtos, PageRequest.of(filterDto.getPage(), defaultSize), allCartItems.size());
    }

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
            cartItem.setSelected(true); // Ensure it's selected when quantity is updated/added
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
            cartItem.setSelected(true); // Default to selected when adding new item
        }

        cartItemRepository.save(cartItem);
        // cartRepository.save(userCart); // No need to save cart explicitly here,
        // as cartItem.setCart(userCart) already links them and userCart is managed.

        return mapCartToCartResponseDto(userCart); // Return the full updated cart
    }

    @Transactional
    public CartResponseDto removeProductFromCart(User user, Long productId) {

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not in cart."));

        userCart.getCartItems().remove(cartItem); // This will trigger orphanRemoval

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

        // cartRepository.save(userCart); // No need to save cart explicitly if cartItem is managed.

        return mapCartToCartResponseDto(userCart);
    }

    // --- НОВЫЙ МЕТОД: Обновление состояния выбранности товара в корзине ---
    @Transactional
    public CartResponseDto updateCartItemSelection(User user, Long productId, boolean selected) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Корзина не найдена для пользователя: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Товар не найден в корзине."));

        cartItem.setSelected(selected); // Устанавливаем новое состояние выбранности
        cartItemRepository.save(cartItem); // Сохраняем изменение

        return mapCartToCartResponseDto(userCart); // Возвращаем обновленную сводку корзины
    }

    @Transactional
    public CartResponseDto purchaseCart(User user) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Корзина не найдена для пользователя: " + user.getUsername()));

        // Фильтруем только выбранные товары для покупки
        List<CartItem> selectedCartItems = userCart.getCartItems().stream()
                .filter(CartItem::isSelected)
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В вашей корзине нет выбранных товаров для оформления заказа.");
        }

        // Проверка наличия товаров и обновление количества на складе только для выбранных товаров
        for (CartItem item : selectedCartItems) {
            Product product = item.getProduct();
            Integer requestedQuantity = item.getQuantity();

            if (product.getQuantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Количество на складе для продукта '" + product.getName() + "' не определено.");
            }

            if (product.getQuantity() < requestedQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Недостаточно товара '%s' на складе. Доступно: %d, запрошено: %d. Пожалуйста, измените количество или отмените выбор.",
                                product.getName(), product.getQuantity(), requestedQuantity));
            }

            product.setQuantity(product.getQuantity() - requestedQuantity);
            productRepository.save(product);
        }

        // Удаление только выбранных товаров из корзины
        userCart.getCartItems().removeAll(selectedCartItems);
        cartRepository.save(userCart); // Сохраняем корзину после удаления выбранных товаров

        // Возвращаем обновленную корзину, в которой остались только невыбранные товары
        return mapCartToCartResponseDto(userCart);
    }

    // Изменяем mapCartToCartResponseDto, чтобы он вычислял итоги только для выбранных товаров
    private CartResponseDto mapCartToCartResponseDto(Cart cart) {
        List<CartItem> allCartItems = cart.getCartItems() != null ? cart.getCartItems().stream().toList() : Collections.emptyList();

        // Мы всегда маппим ВСЕ элементы в DTO, но расчет totalCost/totalQuantity будет идти только по selected = true
        List<CartItemDto> itemDtos = allCartItems.stream()
                .map(item -> {
                    CartItemDto dto = new CartItemDto(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getProductImage(),
                            item.getProduct().getPrice(),
                            item.getQuantity(),
                            item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                            item.isSelected()
                    );
                    return dto;
                })
                .collect(Collectors.toList());

        // Расчет общей стоимости и количества ТОЛЬКО для выбранных товаров
        BigDecimal totalCost = itemDtos.stream()
                .filter(CartItemDto::isSelected) // Filter by selected = true
                .map(CartItemDto::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalQuantity = itemDtos.stream()
                .filter(CartItemDto::isSelected) // Filter by selected = true
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        return new CartResponseDto(
                cart.getId(),
                cart.getUser().getId(),
                itemDtos, // Return all items, so the frontend can display them all
                totalCost,
                totalQuantity,null
        );
    }
}