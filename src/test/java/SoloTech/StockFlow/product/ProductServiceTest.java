package SoloTech.StockFlow.product;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.exception.ProductNotFoundException;
import SoloTech.StockFlow.product.repository.ProductRepository;
import SoloTech.StockFlow.product.service.ProductService;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CachePublisher cachePublisher;

    @InjectMocks
    private ProductService productService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // ✅ Redis Mock 설정
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());
    }

    @Test
    @DisplayName("제품 생성")
    void createProductTest() {
        // Given
        ProductDto productDto = new ProductDto("새로운 가방", 15000L, "튼튼한 여행용 가방.");
        Product mockProduct = new Product(1L, "P002", "새로운 가방", 15000L, "튼튼한 여행용 가방.");

        Snowflake snowflake = new Snowflake(1, 1);
        mockProduct.setProductId(String.valueOf(snowflake.nextId()));

        when(objectMapper.convertValue(productDto, Product.class)).thenReturn(mockProduct);
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);

        // When
        Product createdProduct = productService.createProduct(productDto);

        // Then
        assertNotNull(createdProduct);
        assertEquals("새로운 가방", createdProduct.getName());
        assertEquals(15000L, createdProduct.getPrice());

        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("제품 조회")
    void getProductTest() {
        // Given
        String productId = "P001";
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(mockProduct));

        // When
        Product result = productService.getProduct(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertEquals("가방", result.getName());

        verify(productRepository, times(1)).findByProductId(productId);
    }

    @Test
    @DisplayName("제품 수정 시")
    void updateProductTest() throws Exception {
        // Given
        String productId = "P001";
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");
        ProductDto updateDto = new ProductDto("수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // DTO 값을 기존 객체에 반영하는 mock 설정
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ProductDto dto = invocation.getArgument(1);
            product.setName(dto.getName());
            product.setPrice(dto.getPrice());
            product.setContent(dto.getContent());
            return null;
        }).when(objectMapper).updateValue(any(Product.class), any(ProductDto.class));

        // When
        Product updatedProduct = productService.updateProduct(productId, updateDto);

        // Then
        assertNotNull(updatedProduct);
        assertEquals("수정된 가방", updatedProduct.getName());
        assertEquals(12000L, updatedProduct.getPrice());
        assertEquals("더 실용적이고 멋진 백팩.", updatedProduct.getContent());

        // 캐시 및 DB 저장 검증
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("제품이 없는 경우 수정할 경우")
    void updateProduct_NoProduct() {
        String productId = "NOT_FOUND";
        ProductDto updateDto = new ProductDto("수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class, () ->
                productService.updateProduct(productId, updateDto));

        assertTrue(exception.getMessage().contains("Product not found"));

        verify(productRepository).findByProductId(productId);
    }

    @Test
    @DisplayName("제품 삭제")
    void deleteProductTest() {
        // Given
        String productId = "P001";
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(mockProduct));
        doNothing().when(productRepository).delete(mockProduct);

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository, times(1)).delete(mockProduct);
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
