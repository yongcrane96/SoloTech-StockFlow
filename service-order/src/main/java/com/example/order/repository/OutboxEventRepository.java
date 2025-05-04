package com.example.order.repository;

import com.example.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;


@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByAggregateIdAndPublishedTrue(String aggregateId, boolean published);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findByPublishedFalse();
}