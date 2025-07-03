// src/main/java/com/example/spring/service/busket/CartService.java
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
import com.example.spring.service.payment.PaymentService; // Import the interface
import com.example.spring.service.payment.PaymentServiceFactory; // Import the factory
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    private final PaymentServiceFactory paymentServiceFactory; // <-- Inject the factory!

    @Value("${list.products.pagination.default.page}")
    private int defaultPage;
    @Value("${list.products.pagination.default.size}")
    private int defaultSize;

    // These URLs can be common for the frontend if it can work with different systems.
    // If each payment system requires its own URLs, they will need to be managed within each PaymentService
    // or passed specific URLs for the payment system via CartService.
    @Value("${paypal.frontend.success-url}") // Rename to a more general name if planning for other payment systems
    private String frontendSuccessUrl;

    @Value("${paypal.frontend.cancel-url}") // Rename to a more general name
    private String frontendCancelUrl;

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    /**
     * Initiates a payment for selected items in the user's cart using a specified payment method.
     *
     * @param user The authenticated user.
     * @param paymentMethod The desired payment method (e.g., "paypal", "stripe").
     * @return A map containing the "redirect_url" for the user to proceed with payment.
     * @throws ResponseStatusException if no items are selected, total amount is invalid,
     * or stock is insufficient.
     */
    @Transactional
    public Map<String, String> purchaseCart(User user, String paymentMethod) { // <-- paymentMethod added
        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        List<CartItem> selectedCartItems = userCart.getCartItems().stream()
                .filter(CartItem::isSelected)
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No selected items in your cart for checkout.");
        }

        BigDecimal totalAmount = selectedCartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot process payment with zero or negative total amount.");
        }

        // --- Stock Check (BEFORE initiating payment) ---
        for (CartItem item : selectedCartItems) {
            Product product = item.getProduct();
            Integer requestedQuantity = item.getQuantity();

            if (product.getQuantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity for product '" + product.getName() + "' is not defined.");
            }

            if (product.getQuantity() < requestedQuantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Insufficient stock for product '%s'. Available: %d, requested: %d. Please adjust quantity or unselect the item.",
                                product.getName(), product.getQuantity(), requestedQuantity));
            }
        }

        // --- Initiate Payment using the Factory ---
        try {
            PaymentService paymentService = paymentServiceFactory.getPaymentService(paymentMethod); // <-- Get the required service

            // URLs for redirection will now depend on the chosen service or be universal.
            // For simplicity, we currently use the same URLs, but in a real application, they might be specific to each system.
            String successUrlWithUserId = frontendSuccessUrl + "?userId=" + user.getId();
            String cancelUrlWithUserId = frontendCancelUrl + "?userId=" + user.getId();

            Map<String, String> response = paymentService.initiatePayment( // <-- Call via the interface
                    totalAmount,
                    "USD", // Currency (can be configurable)
                    "Purchase from your store", // Description
                    cancelUrlWithUserId,
                    successUrlWithUserId
            );
            return response;

        } catch (IllegalArgumentException e) { // Catch our exceptions
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected errors
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during payment initiation: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a successful payment by deducting stock and clearing purchased items from the cart.
     * This method should be called by a specific PaymentController ONLY after a payment system confirms a successful payment.
     *
     * @param user The user for whom the payment was made.
     * @param paypalTransactionId The transaction ID from PayPal (for logging/auditing/order tracking).
     * @throws ResponseStatusException if cart or items are not found, or stock deduction fails.
     */
    @Transactional
    public void finalizePurchase(User user, String paypalTransactionId) { // Renamed paymentId to paypalTransactionId for clarity
        logger.info("Finalizing purchase for user {} with PayPal Transaction ID: {}", user.getId(), paypalTransactionId);

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    logger.error("Cart not found for user {} during finalization of transaction {}.", user.getId(), paypalTransactionId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername());
                });

        List<CartItem> purchasedItems = userCart.getCartItems().stream()
                .filter(CartItem::isSelected)
                .collect(Collectors.toList());

        if (purchasedItems.isEmpty()) {
            logger.warn("No selected items found in cart for user {} for PayPal transaction {}. Order might have been processed already or cart changed.", user.getId(), paypalTransactionId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No selected items found in cart for user " + user.getUsername() + " to finalize payment " + paypalTransactionId + ". Order might have been processed already or cart changed.");
        }

        // --- Deduct Stock (ONLY NOW, after confirmed payment) ---
        for (CartItem item : purchasedItems) {
            Product product = item.getProduct();
            Integer requestedQuantity = item.getQuantity();

            logger.info("Deducting stock for product '{}' (ID: {}) for transaction {}. Requested: {}, Available: {}",
                    product.getName(), product.getId(), paypalTransactionId, requestedQuantity, product.getQuantity());

            if (product.getQuantity() == null || product.getQuantity() < requestedQuantity) {
                logger.error("Critical stock error for product '{}' (ID: {}) during finalization of payment {}. Available: {}, purchased: {}. MANUAL REVIEW REQUIRED.",
                        product.getName(), product.getId(), paypalTransactionId, product.getQuantity(), requestedQuantity);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Critical stock error for product '%s' during finalization of payment %s. Available: %d, purchased: %d. MANUAL REVIEW REQUIRED.",
                                product.getName(), paypalTransactionId, product.getQuantity(), requestedQuantity));
            }
            product.setQuantity(product.getQuantity() - requestedQuantity);
            productRepository.save(product);
            logger.info("Stock for product '{}' (ID: {}) updated. New quantity: {}", product.getName(), product.getId(), product.getQuantity());
        }

        // --- Clear Purchased Items from Cart ---
        cartItemRepository.deleteAll(purchasedItems); // Delete cart items directly
        userCart.getCartItems().removeAll(purchasedItems); // Update the collection in the cart object
        cartRepository.save(userCart); // Save the cart so that changes to the collection are persisted

        logger.info("Purchase finalized successfully for user {} with PayPal Transaction ID: {}. Cart items removed.", user.getId(), paypalTransactionId);
        // In a real application, an Order object would also be created or updated here
        // For example: orderService.createOrder(user, purchasedItems, paypalTransactionId, totalAmount);
    }

    // ... (rest of the helper methods mapCartToCartResponseDto etc. unchanged) ...

    /**
     * Retrieves the current user's cart details.
     * If the user has no cart, an empty CartResponseDto is returned.
     *
     * @param user Authenticated user.
     * @return CartResponseDto containing cart details, or empty if cart does not exist.
     */
    @Transactional(readOnly = true)
    public CartResponseDto getUserCart(User user) {
        return cartRepository.findByUser(user)
                .map(this::mapCartToCartResponseDto)
                .orElseGet(() -> new CartResponseDto(null, null, Collections.emptyList(), BigDecimal.ZERO, 0, null));
    }

    /**
     * Retrieves a paginated list of cart items for a given user
     * with optional product name filtering.
     *
     * @param user Authenticated user.
     * @param filterDto DTO containing product name for search, page number, size, and sort parameters.
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

        // 1. Optional filtering by product name
        if (filterDto.getProductName() != null && !filterDto.getProductName().trim().isEmpty()) {
            String searchTerm = filterDto.getProductName().trim().toLowerCase();
            allCartItems = allCartItems.stream()
                    .filter(item -> item.getProduct().getName().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }

        // 2. Manual pagination (since we're filtering/sorting in-memory in this example)
        // Ensure filterDto.getPage() and defaultSize are used correctly for pagination calculation
        int start = (int) PageRequest.of(filterDto.getPage(), defaultSize).getOffset();
        int end = Math.min((start + defaultSize), allCartItems.size());

        // Handle cases where start index is out of bounds of the list size
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
     * Adds a product to the user's cart or updates its quantity if it already exists.
     *
     * @param user Authenticated user.
     * @param requestDto DTO containing productId and quantity to add.
     * @return Updated CartResponseDto.
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
                        String.format("Insufficient stock for product '%s'. Available: %d, attempting to add: %d. Total in cart will be: %d.",
                                product.getName(), product.getQuantity(), quantityToAdd, newQuantity));
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setSelected(true); // Ensure it's selected when updating/adding quantity
        } else {
            if (product.getQuantity() != null && product.getQuantity() < quantityToAdd) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Insufficient stock for product '%s'. Available: %d, attempting to add: %d.",
                                product.getName(), product.getQuantity(), quantityToAdd));
            }
            cartItem = new CartItem();
            cartItem.setCart(userCart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantityToAdd);
            cartItem.setSelected(true); // Default to selected when adding a new item
        }

        cartItemRepository.save(cartItem);

        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Removes a product from the user's cart.
     *
     * @param user Authenticated user.
     * @param productId The ID of the product to remove.
     * @return Updated CartResponseDto.
     * @throws ResponseStatusException if cart or product is not found.
     */
    @Transactional
    public CartResponseDto removeProductFromCart(User user, Long productId) {

        Cart userCart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + user.getUsername()));

        Product product = repositoryService.loadProductById(productId);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(userCart, product)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in cart."));

        userCart.getCartItems().remove(cartItem);

        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Updates the quantity of a specific item in the user's cart.
     * If the new quantity is zero or less, the item is removed from the cart.
     *
     * @param user Authenticated user.
     * @param requestDto DTO containing the product ID and the new quantity.
     * @return Updated CartResponseDto.
     * @throws ResponseStatusException if cart or product is not found, or insufficient stock.
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
                    String.format("Insufficient stock for product '%s'. Available: %d, attempting to set quantity: %d.",
                            product.getName(), product.getQuantity(), newQuantity));
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem); // Save the updated cart item

        return mapCartToCartResponseDto(userCart);
    }

    /**
     * Updates the selection status (selected/unselected) of a product in the user's cart.
     *
     * @param user Authenticated user.
     * @param productId The ID of the product whose selection status needs to be updated.
     * @param selected A boolean indicating whether the product should be selected (true) or unselected (false).
     * @return The updated CartResponseDto.
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

        return mapCartToCartResponseDto(userCart); // Return summary of the updated cart
    }


    private CartResponseDto mapCartToCartResponseDto(Cart cart) {
        List<CartItem> allCartItems = cart.getCartItems() != null ? cart.getCartItems().stream().toList() : Collections.emptyList();

        // Convert all cart items to DTOs, including their selection status
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
                itemDtos, // Return all items so frontend can display them all (selected and unselected)
                totalCost,
                totalQuantity,
                null // errorMessage is typically handled by exceptions
        );
    }
}