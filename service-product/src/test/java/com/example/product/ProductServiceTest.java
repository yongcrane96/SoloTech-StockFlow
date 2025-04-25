package com.example.product;

import cn.hutool.core.lang.Snowflake;
import com.example.cache.CachePublisher;
import com.example.kafka.CreateProductEvent;
import com.example.kafka.UpdateProductEvent;
import com.example.product.dto.ProductDto;
import com.example.product.entity.Product;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.repository.ProductRepository;
import com.example.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product defaultProduct;
    private CreateProductEvent defaultCreateEvent;
    private UpdateProductEvent defaultUpdateEvent;
    private String productId = "P001";

    @BeforeEach
    void setUp() {
        Snowflake snowflake = new Snowflake(1, 1);
        long snowflakeId = snowflake.nextId();

        defaultCreateEvent = new CreateProductEvent(snowflakeId,
                productId,"가방",10000L,"실용성 높은 백팩.");

        defaultUpdateEvent = new UpdateProductEvent(
                productId,"수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");

        defaultProduct = Product.builder()
                .id(snowflakeId)
                .productId(productId)
                .name("가방")
                .price(10000L)
                .content("실용성 높은 백팩.")
                .build();
    }

    @Test
    @DisplayName("제품 생성 테스트")
    void createProductSuccessTest() {
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(defaultProduct);

        Product result = productService.createProduct(defaultCreateEvent);

        assertNotNull(result);
        assertEquals(defaultProduct.getProductId(), result.getProductId());
        assertEquals(defaultProduct.getName(), result.getName());
        assertEquals(defaultProduct.getPrice(), result.getPrice());
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
    }
    @Test
    @DisplayName("제품 생성 테스트 - 예외 발생 및 보상 처리")
    void createProductFailureTest() {
        // given
        when(productRepository.saveAndFlush(any(Product.class))).thenThrow(new RuntimeException("DB 저장 실패"));
        doNothing().when(productRepository).deleteById(anyLong());

        // when & then
        Exception exception = assertThrows(ProductNotFoundException.class, () -> {
            productService.createProduct(defaultCreateEvent);
        });

        assertTrue(exception.getMessage().contains(productId), "예외 메시지가 올바르지 않습니다.");
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        verify(productRepository, times(1)).deleteById(defaultCreateEvent.getId());
    }

    @Test
    @DisplayName("제품 조회")
    void getProductTest() {
        // Given
        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(defaultProduct));

        // When
        Product result = productService.getProduct(productId);

        // Then
        assertNotNull(result);
        assertEquals(defaultProduct.getProductId(), result.getProductId());
        assertEquals(defaultProduct.getName(), result.getName());
        assertEquals(defaultProduct.getPrice(), result.getPrice());
        verify(productRepository, times(1)).findByProductId(productId);
    }

    @Test
    @DisplayName("제품 수정 시")
    void updateProductTest() {
        when(productRepository.findByProductId(defaultUpdateEvent.getProductId())).thenReturn(Optional.of(defaultProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updatedProduct = productService.updateProduct(defaultUpdateEvent);

        assertNotNull(updatedProduct);
        assertEquals("수정된 가방", updatedProduct.getName());
        assertEquals(12000L, updatedProduct.getPrice());
        assertEquals("더 실용적이고 멋진 백팩.", updatedProduct.getContent());

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("제품이 없는 경우 수정할 경우")
    void updateProduct_NoProduct() {
        String productId = "NOT_FOUND";
        defaultUpdateEvent = new UpdateProductEvent(
                productId,"수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class, () ->
                productService.updateProduct(defaultUpdateEvent));

        assertTrue(exception.getMessage().contains("Product not found"));
        verify(productRepository).findByProductId(productId);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("제품 삭제")
    void deleteProductTest() {
        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(defaultProduct));
        doNothing().when(productRepository).delete(defaultProduct);

        productService.deleteProduct(productId);

        verify(productRepository, times(1)).delete(defaultProduct);
    }

    @Test
    @DisplayName("제품이 없는 경우 삭제할 경우")
    void deleteProduct_NoProduct() {
        String productId = "NOT_FOUND";
        when(productRepository.findByProductId(productId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(ProductNotFoundException.class, () ->
                productService.deleteProduct(productId));

        assertTrue(exception.getMessage().contains("Product not found"));

        verify(productRepository).findByProductId(productId);
    }
}
