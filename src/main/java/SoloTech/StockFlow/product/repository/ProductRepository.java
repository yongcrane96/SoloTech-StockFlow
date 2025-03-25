package SoloTech.StockFlow.product.repository;

import SoloTech.StockFlow.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상품 레퍼지토리
 *
 * @since   2025-03-25
 * @author  yhkim
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductId(String productId);
}
