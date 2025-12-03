package com.ecommerce.cart_service3.repository;

import com.ecommerce.cart_service3.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByProductId(Long productId);
}