package SoloTech.StockFlow.store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상점 객체
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@Entity
@Data
@Table(name = "stores")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Store {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String storeId;
    private String storeName;
    private String address;
}
