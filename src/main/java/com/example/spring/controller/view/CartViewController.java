package com.example.spring.controller.view;

import com.example.spring.dto.cart.CartFilterDto;
import com.example.spring.dto.cart.CartItemDto;
import com.example.spring.dto.cart.CartResponseDto;
import com.example.spring.entity.User;
import com.example.spring.service.busket.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller // This annotation indicates it's a Spring MVC controller returning views
@RequiredArgsConstructor
@RequestMapping("/cart") // Base path for cart views
public class CartViewController {

    private final CartService cartService;

    /**
     * Handles requests to display the user's shopping cart page.
     * It fetches cart items with pagination and filtering and adds them to the model for the view.
     *
     * @param currentUser The authenticated user.
     * @param filterDto DTO for pagination and filtering options.
     * @param model The Spring Model to pass data to the view.
     * @return The name of the Thymeleaf template for the cart page.
     */
    @GetMapping // Maps to /cart
    public String showCartPage(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute CartFilterDto filterDto,
            Model model) {

        if (currentUser == null) {
            // If user is not authenticated, redirect to login or show an empty cart/message
            // For simplicity, let's redirect to login page if user is not authenticated
            return "redirect:/login"; // Assuming you have a /login endpoint
        }

        // Fetch paginated cart items using the service
        Page<CartItemDto> cartItemsPage = cartService.getUserCartItems(currentUser, filterDto);

        // Fetch overall cart summary for displaying total cost/quantity in a cart summary section
        CartResponseDto cartSummary = cartService.getUserCart(currentUser);

        // Add data to the model to be accessible in the Thymeleaf template
        model.addAttribute("cartItemsPage", cartItemsPage);
        model.addAttribute("cartSummary", cartSummary);
        model.addAttribute("filterDto", filterDto); // Pass filterDto back to the view to populate form fields

        // Return the name of the Thymeleaf template (e.g., src/main/resources/templates/cart.html)
        return "cart/cart"; // This will resolve to /templates/cart.html
    }

    /**
     * Displays the checkout page.
     *
     * @param currentUser The authenticated user.
     * @param model The Spring Model to pass data to the view.
     * @return The name of the Thymeleaf template for the checkout page.
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(@AuthenticationPrincipal User currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        CartResponseDto cart = cartService.getUserCart(currentUser);
        model.addAttribute("cart", cart);
        return "checkout"; // Resolves to /templates/checkout.html
    }

    /**
     * Handles requests for the successful payment page.
     *
     * @param userId The ID of the user.
     * @param paymentId The payment ID from the payment gateway.
     * @param transactionId The transaction ID from the payment gateway.
     * @param model The Spring Model to pass data to the view.
     * @return The name of the Thymeleaf template for the success payment page.
     */
    @GetMapping("/success-payment")
    public String handleSuccessPayment(
            @RequestParam("userId") Long userId,
            @RequestParam("paymentId") String paymentId,
            @RequestParam("transactionId") String transactionId,
            Model model) {

        model.addAttribute("userId", userId);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("transactionId", transactionId);
        // Add other data you want to display on the page

        return "cart/success-payment";
    }

    /**
     * Handles requests for the payment error page.
     *
     * @return The name of the Thymeleaf template for the error page.
     */
    @GetMapping("/error-payment")
    public String handleErrorPayment() {
        return "cart/error-page"; // Template name for the error page
    }
}