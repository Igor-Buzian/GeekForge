package com.example.spring.controller;

import com.example.spring.dto.cart.CartFilterDto;
import com.example.spring.dto.cart.CartItemDto;
import com.example.spring.dto.cart.CartItemRequestDto;
import com.example.spring.dto.cart.CartResponseDto;
import com.example.spring.entity.User;
import com.example.spring.service.busket.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    /**
     * Endpoint for displaying cart contents with pagination and optional filtering by product name.
     * When first called (without productName parameter), it returns all items in the cart.
     *
     * @param currentUser The authenticated user.
     * @param filterDto DTO containing product name for search, page number, size, and sort options.
     * @return A paginated list of CartItemDto, or 401 if unauthorized.
     */
    @GetMapping("/items")
    public ResponseEntity<Page<CartItemDto>> getUserCartItems(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute CartFilterDto filterDto) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to get cart items.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Fetching cart items for user: {}", currentUser.getId());
        Page<CartItemDto> cartItemsPage = cartService.getUserCartItems(currentUser, filterDto);
        // Returning 200 OK even if the page is empty, as an empty list is a valid response.
        return ResponseEntity.ok(cartItemsPage);
    }

    /**
     * Retrieves general cart information (total cost, total quantity, and full list of items).
     * This is intended for getting a complete summary of the cart.
     *
     * @param currentUser The authenticated user.
     * @return A CartResponseDto summarizing the user's cart, or 401 if unauthorized.
     */
    @GetMapping("/summary")
    public ResponseEntity<CartResponseDto> getCartSummary(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to get cart summary.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Fetching cart summary for user: {}", currentUser.getId());
        CartResponseDto cart = cartService.getUserCart(currentUser);
        // Always return 200 OK, even if the cart is empty, with an empty DTO.
        return ResponseEntity.ok(cart);
    }

    /**
     * Endpoint for adding a product to the cart.
     *
     * @param currentUser The authenticated user.
     * @param requestDto DTO containing product ID and quantity to add.
     * @return An updated CartResponseDto, or appropriate error status if invalid.
     */
    @PostMapping("/add")
    public ResponseEntity<CartResponseDto> addProductToCart(
            @AuthenticationPrincipal User currentUser,
            @RequestBody CartItemRequestDto requestDto) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to add product {} to cart.", requestDto.getProductId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Adding product {} with quantity {} to cart for user {}",
                requestDto.getProductId(), requestDto.getQuantity(), currentUser.getId());
        // CartService methods throw ResponseStatusException, which Spring will handle
        CartResponseDto updatedCart = cartService.addProductToCart(currentUser, requestDto);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Endpoint for removing a product from the cart.
     *
     * @param currentUser The authenticated user.
     * @param productId The ID of the product to remove.
     * @return An updated CartResponseDto, or appropriate error status if invalid.
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponseDto> removeProductFromCart(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to remove product {} from cart.", productId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Removing product {} from cart for user {}.", productId, currentUser.getId());
        // CartService methods throw ResponseStatusException, which Spring will handle
        CartResponseDto updatedCart = cartService.removeProductFromCart(currentUser, productId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Endpoint for updating the quantity of a product in the cart.
     * Can be used for '+' and '-' buttons.
     *
     * @param currentUser The authenticated user.
     * @param requestDto DTO containing product ID and new quantity.
     * @return An updated CartResponseDto, or appropriate error status if invalid.
     */
    @PutMapping("/update-quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @AuthenticationPrincipal User currentUser,
            @RequestBody CartItemRequestDto requestDto) { // productId and newQuantity
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to update quantity for product {} in cart.", requestDto.getProductId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Updating quantity for product {} to {} for user {}.",
                requestDto.getProductId(), requestDto.getQuantity(), currentUser.getId());
        // CartService methods throw ResponseStatusException, which Spring will handle
        CartResponseDto updatedCart = cartService.updateCartItemQuantity(currentUser, requestDto);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Updates the selection status (selected/unselected) of a product in the user's cart.
     *
     * @param currentUser The authenticated user.
     * @param productId The ID of the product whose selection status is to be updated.
     * @param selected A boolean indicating whether the product should be selected (true) or unselected (false).
     * @return An updated CartResponseDto with recalculated totals, or appropriate error status.
     */
    @PutMapping("/update-selection")
    public ResponseEntity<CartResponseDto> updateCartItemSelection(
            @AuthenticationPrincipal User currentUser,
            @RequestParam Long productId,
            @RequestParam boolean selected) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to update cart selection for productId: {}.", productId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        logger.info("Attempting to update selection for userId: {} productId: {} to selected: {}", currentUser.getId(), productId, selected);
        // CartService method throws ResponseStatusException, which Spring will handle
        CartResponseDto updatedCart = cartService.updateCartItemSelection(currentUser, productId, selected);
        logger.info("Successfully updated selection for userId: {} productId: {}. Returning updated cart summary.", currentUser.getId(), productId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Processes the purchase of selected items in the user's cart.
     * This method validates stock, reduces product quantities, and removes purchased items from the cart.
     *
     * @param currentUser The authenticated user.
     * @return An updated CartResponseDto showing remaining items in the cart (or an empty cart),
     * or appropriate error status if the purchase fails.
     */
    @PostMapping("/purchase")
    public ResponseEntity<CartResponseDto> purchaseCart(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to purchase cart.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }
        logger.info("Attempting to purchase cart for user: {}", currentUser.getId());
        // CartService method throws ResponseStatusException, which Spring will handle
        CartResponseDto purchasedCart = cartService.purchaseCart(currentUser);
        logger.info("Cart purchase successful for user: {}. Remaining items in cart: {}", currentUser.getId(), purchasedCart.getCartItems().size());
        return ResponseEntity.ok(purchasedCart);
    }
}