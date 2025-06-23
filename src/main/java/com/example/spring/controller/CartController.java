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

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    /**
     * Endpoint for displaying cart contents with pagination and optional filtering by product name.
     * When first called (without productName parameter), it returns all items in the cart.
     */
    @GetMapping("/items")
     public ResponseEntity<Page<CartItemDto>> getUserCartItems(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute CartFilterDto filterDto) { // @ModelAttribute allows binding request parameters to DTO
        if (currentUser == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        try
        {
            // Pass userId from currentUser to the service and the filter DTO
            Page<CartItemDto> cartItemsPage = cartService.getUserCartItems(currentUser, filterDto);
            if (cartItemsPage.isEmpty()) {
                return ResponseEntity.noContent().build(); // 204 No Content if cart is empty or no items match filters
            }
            return ResponseEntity.ok(cartItemsPage);
        } catch (Exception e) {
            System.err.println("Error: "+e.getMessage());
           return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Retrieves general cart information (total cost, total quantity, and full list of items).
     * This is intended for getting a complete summary of the cart.
     */
    @GetMapping("/summary")
    public ResponseEntity<CartResponseDto> getCartSummary(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        // Используем mapCartToCartResponseDto, чтобы получить общие итоги,
        // но без фильтрации и пагинации, как в getUserCartItems.
        // Примечание: getUserCart в CartService возвращает CartResponseDto
        CartResponseDto cart = cartService.getUserCart(currentUser);
        if (cart == null || cart.getCartItems().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cart);
    }

    // Endpoint for adding a product to the cart
    @PostMapping("/add")
    public ResponseEntity<CartResponseDto> addProductToCart(
            @AuthenticationPrincipal User currentUser,
            @RequestBody CartItemRequestDto requestDto) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
       try {
           CartResponseDto updatedCart = cartService.addProductToCart(currentUser, requestDto);
           return ResponseEntity.ok(updatedCart);
       }catch (Exception e)
       {
           System.err.println("Error: "+e.getMessage());
           return ResponseEntity.status(401).build();
       }
    }

    // Endpoint for removing a product from the cart
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponseDto> removeProductFromCart(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            CartResponseDto updatedCart = cartService.removeProductFromCart(currentUser, productId);
            return ResponseEntity.ok(updatedCart);
        }catch (Exception e)
        {
            System.err.println("Error: "+e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    // Endpoint for updating the quantity of a product in the cart
    // Can be used for '+' and '-' buttons
    @PutMapping("/update-quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @AuthenticationPrincipal User currentUser,
            @RequestBody  CartItemRequestDto requestDto) { // productId and newQuantity
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            CartResponseDto updatedCart = cartService.updateCartItemQuantity(currentUser, requestDto);
            return ResponseEntity.ok(updatedCart);
        }
        catch (Exception e)
        {
            System.err.println("Error: "+e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }


    @PutMapping("/update-selection")
    public ResponseEntity<Void> updateCartItemSelection(
            @AuthenticationPrincipal User currentUser, // <-- ДОБАВЛЕНО
            @RequestParam Long productId,
            @RequestParam boolean selected) {
        // Проверка на null, если пользователь может быть не аутентифицирован
        if (currentUser == null) {
            logger.warn("Unauthorized attempt to update cart selection for productId: {}", productId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        logger.info("Attempting to update selection for userId: {} productId: {} to selected: {}", currentUser.getId(), productId, selected);
        try {
            // ПЕРЕДАЙТЕ currentUser в сервис
            cartService.updateCartItemSelection(currentUser, productId, selected);
            logger.info("Successfully updated selection for userId: {} productId: {}", currentUser.getId(), productId);
            return ResponseEntity.ok().build();
        }  catch (Exception e) {
            logger.error("Error updating selection for userId: {} productId: {}. Error: {}", currentUser.getId(), productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity<CartResponseDto> purchaseCart(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        try {
            CartResponseDto purchasedCart = cartService.purchaseCart(currentUser);
            // Если покупка прошла успешно и корзина очищена,
            // можно вернуть 200 OK с пустой корзиной или просто 204 No Content.
            // Возвращаем 200 OK с обновленной (пустой) корзиной.
            return ResponseEntity.ok(purchasedCart);
        } catch (Exception e) {
            System.err.println("Error during cart purchase: " + e.getMessage());
            // Если это ResponseStatusException, то можно извлечь статус и сообщение
            if (e instanceof org.springframework.web.server.ResponseStatusException) {
               // HttpStatus status = ((org.springframework.web.server.ResponseStatusException) e).getStatusCode();
                String reason = ((org.springframework.web.server.ResponseStatusException) e).getReason();
                // Возвращаем статус ошибки, который был выброшен из сервиса
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CartResponseDto(null, null, null, null, 0, null));
            }
            // Для других ошибок
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CartResponseDto(null, null, null, null, 0, null));
        }
    }
}