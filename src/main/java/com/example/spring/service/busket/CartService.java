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
    private final UserRepository userRepository; // This might not be directly used if user is passed from security context
    private final RepositoryService repositoryService;

    @Value("${list.products.pagination.default.page}")
    private int defaultPage;
    @Value("${list.products.pagination.default.size}")
    private int defaultSize;

    /**
     * Retrieves the comprehensive details of a user's cart.
     * If the user has no cart, an empty CartResponseDto is returned.
     *
     * @param user The authenticated user.
     * @return A CartResponseDto containing cart details, or an empty one if no cart exists.
     */
    @Transactional(readOnly = true)
    public CartResponseDto getUserCart(User user) {
        return cartRepository.findByUser(user)
                .map(this::mapCartToCartResponseDto)
                .orElseGet(() -> new CartResponseDto(null, null, Collections.emptyList(), BigDecimal.ZERO, 0, null));
    }

    /**
     * Retrieves a paginated list of cart items for a given user, with optional filtering by product name.
     *
     * @param user The authenticated user.
     * @param filterDto DTO containing product name for search, page number, size, and sort options.
     * @return A Page of CartItemDto representing the items in the user's cart.
     */
    @Transactional(readOnly = true)
    public Page<CartItemDto> getUserCartItems(User user, CartFilterDto filterDto) {

        Cart userCart = cartRepository.findByUser(user)
                .orElse(null);

        // If the cart doesn't exist or is empty, return an empty page.
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

        // 2. Manual Pagination (since we're filtering/sorting in memory for this example)
        // Ensure that filterDto.getPage() and defaultSize are used correctly for pagination calculation
        int start = (int) PageRequest.of(filterDto.getPage(), defaultSize).getOffset();
        int end = Math.min((start + defaultSize), allCartItems.size());

        // Handle cases where the start index is beyond the list size
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
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        item.isSelected()
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(itemDtos, PageRequest.of(filterDto.getPage(), defaultSize), allCartItems.size());
    }

    /**
     * Adds a product to the user's cart or updates the quantity if it already exists.
     *
     * @param user The authenticated user.
     * @param requestDto DTO containing product ID and quantity to add.
     * @return An updated CartResponseDto.
     * @throws ResponseStatusException if quantity is invalid or product is out of stock.
     */
    @Transactional
    public CartResponseDto addProductToCart(User user, CartItemRequestDto requestDto) {

        Product product = repositoryService.loadProductById(requestDto.getProductId());

        if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a positive number.");
        }

        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    // Create a new cart if the user doesn't have one
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
                        String.format("Not enough product '%s' in stock. Available: %d, attempting to add: %d. Total in cart would be: %d.",
                                product.getName(), product.getQuantity(), quantityToAdd, newQuantity));
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setSelected(true); // Ensure it's selected when quantity is updated/added
        } else {
            if (product.getQuantity() != null && product.getQuantity() < quantityToAdd) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Not enough product '%s' in stock. Available: %d, attempting to add: %d.",
                                product.getName(), product.getQuantity(), quantityToAdd));
            }
            cartItem = new CartItem();
            cartItem.setCart(userCart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantityToAdd);
            cartItem.setSelected(true); // Default to selected when adding a new item
        }

        cartItemRepository.save(cartItem);
        // No need to explicitly save userCart here, as cartItem.setCart(userCart) already links them
        // and 'userCart' is a managed entity within the transaction. Changes to its collection will be persisted.

        return mapCartToCartResponseDto(userCart); // Return the full updated cart details
    }

    /**
     * Removes a product from the user's cart.
     *
     * @param user The authenticated user.
     * @param productId The ID of the product to remove.
     * @return An updated CartResponseDto.
     * @throws ResponseStatusException if cart or product is not found.
     */
    @Transactional
    public CartResponseDto removeProductFromCart(User user, Long productId) {

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in cart."));

        userCart.getCartItems().remove(cartItem); // This will trigger orphanRemoval on CartItem

        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Updates the quantity of a specific product in the user's cart.
     * If the new quantity is zero or less, the product is removed from the cart.
     *
     * @param user The authenticated user.
     * @param requestDto DTO containing product ID and the new quantity.
     * @return An updated CartResponseDto.
     * @throws ResponseStatusException if cart or product is not found, or not enough product in stock.
     */
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
            // If quantity is zero or less, remove the item from the cart
            return removeProductFromCart(user, requestDto.getProductId());
        }

        if (product.getQuantity() != null && product.getQuantity() < newQuantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Not enough product '%s' in stock. Available: %d, attempting to set quantity to: %d.",
                            product.getName(), product.getQuantity(), newQuantity));
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem); // Save the updated cart item

        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Updates the selection status (selected/unselected) of a product in the user's cart.
     *
     * @param user The authenticated user.
     * @param productId The ID of the product whose selection status is to be updated.
     * @param selected A boolean indicating whether the product should be selected (true) or unselected (false).
     * @return An updated CartResponseDto.
     * @throws ResponseStatusException if cart or product is not found.
     */
    @Transactional
    public CartResponseDto updateCartItemSelection(User user, Long productId, boolean selected) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in cart."));

        cartItem.setSelected(selected); // Set the new selection status
        cartItemRepository.save(cartItem); // Save the change

        return mapCartToCartResponseDto(userCart); // Return the updated cart summary
    }

    /**
     * Processes the purchase of selected items in the user's cart.
     * This method validates stock, reduces product quantities, and removes purchased items from the cart.
     *
     * @param user The authenticated user.
     * @return An updated CartResponseDto showing remaining items.
     * @throws ResponseStatusException if no items are selected, stock is insufficient, or stock quantity is undefined.
     */
    @Transactional
    public CartResponseDto purchaseCart(User user) {
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        // Filter only the selected items for purchase
        List<CartItem> selectedCartItems = userCart.getCartItems().stream()
                .filter(CartItem::isSelected)
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No selected items in your cart to proceed with the order.");
        }

        // Check stock availability and update product quantities for selected items only
        for (CartItem item : selectedCartItems) {
            Product product = item.getProduct();
            Integer requestedQuantity = item.getQuantity();

            if (product.getQuantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity for product '" + product.getName() + "' is not defined.");
            }

            if (product.getQuantity() < requestedQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Insufficient stock for product '%s'. Available: %d, requested: %d. Please adjust the quantity or deselect the item.",
                                product.getName(), product.getQuantity(), requestedQuantity));
            }

            product.setQuantity(product.getQuantity() - requestedQuantity);
            productRepository.save(product); // Save the updated product quantity
        }

        // Remove only the selected (purchased) items from the cart
        userCart.getCartItems().removeAll(selectedCartItems);
        cartRepository.save(userCart); // Save the cart after removing purchased items

        // Return the updated cart, which now contains only the unselected items
        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Maps a Cart entity to a CartResponseDto, calculating total cost and quantity
     * based *only* on selected items in the cart. All cart items are included in the DTO,
     * but aggregates only reflect selected ones.
     * @param cart The Cart entity to map.
     * @return A CartResponseDto.
     */
    private CartResponseDto mapCartToCartResponseDto(Cart cart) {
        List<CartItem> allCartItems = cart.getCartItems() != null ? cart.getCartItems().stream().toList() : Collections.emptyList();

        // Map all cart items to DTOs, including their selection status
        List<CartItemDto> itemDtos = allCartItems.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getProductImage(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                        item.isSelected()
                ))
                .collect(Collectors.toList());

        // Calculate total cost and total quantity ONLY for selected items
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
                itemDtos, // Return all items, so the frontend can display them all (selected and unselected)
                totalCost,
                totalQuantity,
                null // errorMessage is typically handled by exceptions
        );
    }
}