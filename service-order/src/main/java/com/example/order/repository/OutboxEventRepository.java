package com.example.order.repository;

import com.example.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByAggregateIdAndPublishedTrue(String aggregateId, boolean published);
}