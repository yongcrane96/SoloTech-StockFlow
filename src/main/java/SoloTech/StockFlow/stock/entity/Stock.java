package SoloTech.StockFlow.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 재고 객체
 *
 * @since   2025-03-25
 * @author  yhkim
 */
@Entity
@Data
@Table(name = "stocks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String stockId;
    private String storeId;
    private String productId;
    private Long stock;

    public boolean decrease(long quantity){
        return stock >= quantity && (stock -= quantity) >= 0;
    };


}
