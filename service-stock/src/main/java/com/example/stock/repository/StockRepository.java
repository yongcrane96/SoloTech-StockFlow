package com.example.stock.repository;

import com.example.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockId(String stockId);
    Optional<Stock> findByProductId(String productId);
}
