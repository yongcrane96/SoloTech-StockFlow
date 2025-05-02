package com.example.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String aggregateId;  // 예: orderId
    private String type;    // 예: "OrderCreated", "PaymentRequested"
    private String payload;      // 이벤트의 페이로드 (JSON 형태)
    private boolean published;   // 메시지가 발행되었는지 여부
    private LocalDateTime createdAt;
}
