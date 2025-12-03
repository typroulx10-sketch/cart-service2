package com.ecommerce.cart_service3.controller;

import com.ecommerce.cart_service3.model.Cart;
import com.ecommerce.cart_service3.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart() {
        List<Map<String, Object>> items = cartService.getAllCartItems();
        BigDecimal total = cartService.getCartTotal();
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", total);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> addToCart(@PathVariable Long productId) {
        try {
            Cart cartItem = cartService.addToCart(productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(cartItem);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long cartId) {
        try {
            cartService.removeFromCart(cartId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/empty")
    public ResponseEntity<Void> emptyCart() {
        cartService.emptyCart();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{cartId}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable Long cartId, @RequestBody Map<String, Integer> request) {
        try {
            Integer quantity = request.get("quantity");
            Cart updated = cartService.updateQuantity(cartId, quantity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout() {
        try {
            Map<String, Object> order = cartService.checkout();
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}