package com.example.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByAggregateIdAndPublishedTrue(String aggregateId, boolean published);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findByPublishedFalse();
}