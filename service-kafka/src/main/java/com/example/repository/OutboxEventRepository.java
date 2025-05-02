package com.example.repository;

import com.example.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByAggregateIdAndPublishedTrue(String aggregateId, boolean published);

}
