package com.ecommerce.cart_service3.service;

import com.ecommerce.cart_service3.model.Cart;
import com.ecommerce.cart_service3.repository.CartRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public CartService(CartRepository cartRepository, RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.restTemplate = restTemplate;
    }

    // Get all cart items with product details from Product Service
    public List<Map<String, Object>> getAllCartItems() {
        List<Cart> cartItems = cartRepository.findAll();
        List<Map<String, Object>> enrichedItems = new ArrayList<>();

        for (Cart item : cartItems) {
            String url = productServiceUrl + "/" + item.getProductId();
            Map<String, Object> product = restTemplate.getForObject(url, Map.class);

            Map<String, Object> enriched = new HashMap<>();
            enriched.put("id", item.getId());
            enriched.put("productId", item.getProductId());
            enriched.put("quantity", item.getQuantity());
            enriched.put("addedAt", item.getAddedAt());
            enriched.put("product", product);
            enrichedItems.add(enriched);
        }
        return enrichedItems;
    }

    // Add product to cart
    public Cart addToCart(Long productId) {

        // Verify product exists by calling Product Service
        String url = productServiceUrl + "/" + productId;
        Map<String, Object> product = restTemplate.getForObject(url, Map.class);

        if (product == null) {
            throw new RuntimeException("Product not found with id: " + productId);
        }

        Optional<Cart> existing = cartRepository.findByProductId(productId);
        if (existing.isPresent()) {
            Cart item = existing.get();
            item.setQuantity(item.getQuantity() + 1);
            return cartRepository.save(item);
        } else {
            Cart newItem = new Cart(productId, 1);
            return cartRepository.save(newItem);
        }
    }

    // Remove cart item
    public void removeFromCart(Long cartId) {
        Cart cartItem = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartRepository.delete(cartItem);
    }

    // Empty cart
    public void emptyCart() {
        cartRepository.deleteAll();
    }

    // Calculate total
    public BigDecimal getCartTotal() {
        List<Cart> cartItems = cartRepository.findAll();
        BigDecimal total = BigDecimal.ZERO;

        for (Cart item : cartItems) {
            String url = productServiceUrl + "/" + item.getProductId();
            Map<String, Object> product = restTemplate.getForObject(url, Map.class);
            BigDecimal price = new BigDecimal(product.get("price").toString());
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    // Update quantity
    public Cart updateQuantity(Long cartId, Integer quantity) {
        Cart cartItem = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        cartItem.setQuantity(quantity);
        return cartRepository.save(cartItem);
    }

    // Checkout, send to Order Service and update Inventory
    public Map<String, Object> checkout() {
        List<Cart> cartItems = cartRepository.findAll();
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Prepare order data
        Map<String, Object> orderRequest = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Cart item : cartItems) {
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("productId", item.getProductId());
            orderItem.put("quantity", item.getQuantity());
            items.add(orderItem);
        }
        orderRequest.put("items", items);

        // Send to Order Service
        Map<String, Object> orderResponse = restTemplate.postForObject(
                orderServiceUrl, orderRequest, Map.class);

        // Update Inventory Service
        for (Cart item : cartItems) {
            String inventoryUrl = inventoryServiceUrl + "/" + item.getProductId() + "/reduce";
            Map<String, Integer> inventoryRequest = new HashMap<>();
            inventoryRequest.put("quantity", item.getQuantity());
            restTemplate.put(inventoryUrl, inventoryRequest);
        }

        emptyCart();

        return orderResponse;
    }
}