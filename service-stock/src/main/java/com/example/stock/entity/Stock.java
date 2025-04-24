package com.example.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 재고 객체
 *
 * @since   2025-03-25
 * @author  yhkim
 */
@Entity
@Data
@Table(name = "stocks")
@SQLDelete(sql = "UPDATE stocks SET deleted = true WHERE id = ?") // Hibernate Soft Delete
@Where(clause = "deleted = false") // 자동 필터링
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
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

    @Column(nullable = false)
    private boolean deleted = false; // 기본값 false (소프트 삭제 플래그)

}
