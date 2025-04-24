package com.example.store.repository;

import com.example.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상점 래퍼지토리
 *
 * @since   2025-03-25
 * @author  yhkim
 */
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByStoreId(String storeId);
}
