package com.example.events;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;        // "ProductCreatedEvent", "ProductUpdatedEvent", etc.
    private String payload;          // 이벤트 내용(간단히 JSON or String)
    private LocalDateTime eventTime; // 이벤트 처리된 시각
    private String status;           // "SUCCESS", "FAILED" 등
}
