package SoloTech.StockFlow.stock.repository;

import SoloTech.StockFlow.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockIdAndDeletedFalse(String stockId);
}
