package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 상점 DTO
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@Builder
@Data
@AllArgsConstructor
public class StoreDto {
    private String storeName;
    private String address;

}
