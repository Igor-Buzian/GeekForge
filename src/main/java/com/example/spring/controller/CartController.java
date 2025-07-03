package com.example.spring.controller;

import com.example.spring.dto.cart.CartFilterDto;
import com.example.spring.dto.cart.CartItemDto;
import com.example.spring.dto.cart.CartItemRequestDto;
import com.example.spring.dto.cart.CartResponseDto;
import com.example.spring.entity.User;
import com.example.spring.repository.ProductRepository; // This import might be unused now
import com.example.spring.repository.UserRepository;
import com.example.spring.service.busket.CartService;
import com.example.spring.service.payment.PaymentService;
import com.example.spring.service.payment.PaymentServiceFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model; // This import might be unused now for @RestController
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {
    private final UserRepository userRepository;
    private final CartService cartService;
    private final PaymentServiceFactory paymentServiceFactory;
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
     * Initiates the purchase process for selected items in the user's cart.
     * The chosen payment method is used to process the transaction.
     *
     * @param user The authenticated user.
     * @param paymentMethod The chosen payment method (e.g., "paypal", "stripe").
     * @return A ResponseEntity containing a map with transaction details, potentially including a redirect URL.
     */
    @PostMapping("/purchase")
    public ResponseEntity<Map<String, String>> purchaseCart(@AuthenticationPrincipal User user,
                                                            @RequestParam String paymentMethod) {
        // Log the received payment method for debugging
        System.out.println("Received purchase request with payment method: " + paymentMethod);
        Map<String, String> response = cartService.purchaseCart(user, paymentMethod);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles the success callback from PayPal after a payment approval.
     * This endpoint is typically configured in your PayPal developer settings as a return URL.
     *
     * @param paymentId The ID of the payment from PayPal.
     * @param payerId The ID of the payer from PayPal.
     * @param currentUser The authenticated user, if available.
     * @param userId The ID of the user (if included in your return URL for session re-establishment), used as a fallback.
     * @return A ResponseEntity that triggers a redirect to a user-facing success page or an error page.
     */
    @GetMapping("/payment/success")
    public ResponseEntity<Void> paypalSuccess(@RequestParam("paymentId") String paymentId,
                                              @RequestParam("PayerID") String payerId,
                                              @AuthenticationPrincipal User currentUser,
                                              @RequestParam(value = "userId", required = false) Long userId) {

        User user = currentUser;
        if (user == null && userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for payment finalization."));
        }

        if (user == null) {
            logger.warn("User session expired or not found for payment finalization. Redirecting to error page.");
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/cart/error-payment?message=User session expired or not found. Cannot finalize payment.");
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Found
        }

        try {
            PaymentService paymentService = paymentServiceFactory.getPaymentService("paypal");

            // 1. Execute the payment through the PayPal API
            Map<String, String> paymentExecutionResult = paymentService.executePayment(paymentId, payerId, user);

            // Check the execution result
            if ("approved".equals(paymentExecutionResult.get("status"))) {
                // If the payment is successfully executed by PayPal, finalize the purchase in your system
                String paypalTransactionId = paymentExecutionResult.get("transactionId");

                // Call finalizePurchase, passing the transaction ID
                cartService.finalizePurchase(user, paypalTransactionId);

                logger.info("Payment successful for user {}. Redirecting to success page. PayPal Transaction ID: {}", user.getId(), paypalTransactionId);

                // Create ResponseEntity for redirect
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", "/cart/success-payment?paymentId=" + paymentId + "&transactionId=" + paypalTransactionId + "&userId=" + user.getId());
                return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Found

            } else {
                // Payment was not approved by PayPal
                String errorMessage = "PayPal payment not approved. Status: " + paymentExecutionResult.get("status") + ". " + paymentExecutionResult.getOrDefault("message", "");
                logger.warn("PayPal payment not approved for user {}: {}", user.getId(), errorMessage);
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", "/cart/error-payment?message=" + errorMessage);
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }

        } catch (ResponseStatusException e) {
            logger.error("ResponseStatusException during PayPal success callback for userId {}: {}", user.getId(), e.getReason(), e);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/cart/error-payment?message=" + e.getReason());
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during PayPal success callback for userId {}: {}", user.getId(), e.getMessage(), e);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/cart/error-payment?message=An unexpected error occurred during payment processing: " + e.getMessage());
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    /**
     * Handles the cancel callback from PayPal if a user aborts the payment.
     * This endpoint will redirect the user to a dedicated cancellation page.
     *
     * @param currentUser The authenticated user, if available.
     * @param userId The ID of the user (if included in your cancel URL), used as a fallback.
     * @return A ResponseEntity that triggers a redirect to the cancellation page.
     */
    @GetMapping("/payment/cancel")
    public ResponseEntity<Void> paypalCancel(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "userId", required = false) Long userId) {

        User user = currentUser;
        if (user == null && userId != null) {
            // Try to find the user by userId if not authenticated
            user = userRepository.findById(userId)
                    .orElse(null); // If not found, keep as null
        }

        if (user != null) {
            logger.info("PayPal payment cancelled for user: {}", user.getId());
            // You might want to reset any temporary cart states here if applicable
            // cartService.clearTemporaryPaymentState(user); // Example
        } else {
            logger.warn("PayPal payment cancelled by unauthenticated or unknown user (userId: {}).", userId);
        }

        // Create headers for the redirect
        HttpHeaders headers = new HttpHeaders();
        // Redirect to the View controller to display the cancellation page
        // You can add parameters if you want to display something specific on the cancellation page
        headers.add("Location", "/cart/error-payment?message=PayPal payment was cancelled by user.");
        // Alternatively, if you have a separate cancellation page:
        // headers.add("Location", "/cart/cancel-payment?userId=" + (user != null ? user.getId() : "null"));

        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Found
    }
}