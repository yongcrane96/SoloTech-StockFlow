package SoloTech.StockFlow.product;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.repository.ProductRepository;
import SoloTech.StockFlow.product.service.ProductService;
import SoloTech.StockFlow.stock.entity.Stock;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
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
    private Cache<String, Object> localCache;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CachePublisher cachePublisher;

    @InjectMocks
    private ProductService productService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String PRODUCT_KEY_PREFIX = "product:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // ✅ Redis Mock 설정
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());
    }

    @Test
    void createProductTest() {
        // Given
        ProductDto productDto = new ProductDto("새로운 가방", 15000L, "튼튼한 여행용 가방.");
        Product mockProduct = new Product(1L, "P002", "새로운 가방", 15000L, "튼튼한 여행용 가방.");

        Snowflake snowflake = new Snowflake(1, 1);
        mockProduct.setProductId(String.valueOf(snowflake.nextId()));

        when(objectMapper.convertValue(productDto, Product.class)).thenReturn(mockProduct);
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(mockProduct);
        doNothing().when(cachePublisher).publish(anyString(), anyString());

        // When
        Product createdProduct = productService.createProduct(productDto);

        // Then
        assertNotNull(createdProduct);
        assertEquals("새로운 가방", createdProduct.getName());
        assertEquals(15000L, createdProduct.getPrice());

        // 캐시 키 검증
        String cacheKey = PRODUCT_KEY_PREFIX + createdProduct.getProductId();
        // Redis 및 로컬 캐시에 저장 확인
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(createdProduct), any(Duration.class));
        verify(localCache, times(1)).put(eq(cacheKey), eq(createdProduct));
        verify(cachePublisher).publish(anyString(), anyString());

        // 로그 출력 확인
        System.out.println("Created Product: " + cacheKey);
    }

    @Test
    void getProductTest() {
        // Given
        String productId = "P001";
        String cacheKey = PRODUCT_KEY_PREFIX + productId;
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");

        when(localCache.getIfPresent(cacheKey)).thenReturn(null);
        when(redisTemplate.opsForValue().get(cacheKey)).thenReturn(null);
        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(mockProduct));

        // When
        Product result = productService.getProduct(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertEquals("가방", result.getName());

        verify(localCache, times(1)).getIfPresent(cacheKey);
        verify(redisTemplate.opsForValue(), times(1)).get(cacheKey);
        verify(productRepository, times(1)).findByProductId(productId);
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(mockProduct), any(Duration.class));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockProduct));
    }

    @Test
    void updateProductTest() throws Exception {
        // Given
        String productId = "P001";
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");
        ProductDto updateDto = new ProductDto("수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");

        String cacheKey = PRODUCT_KEY_PREFIX + productId;
        String expectedMessage = "Updated product-" + cacheKey;

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

        // 캐시 저장 Mock
        doNothing().when(valueOperations).set(eq(cacheKey), any(Product.class));
        doNothing().when(cachePublisher).publish(eq("cache-sync"), eq(expectedMessage));

        // When
        Product updatedProduct = productService.updateProduct(productId, updateDto);

        // Then
        assertNotNull(updatedProduct);
        assertEquals("수정된 가방", updatedProduct.getName());
        assertEquals(12000L, updatedProduct.getPrice());
        assertEquals("더 실용적이고 멋진 백팩.", updatedProduct.getContent());

        // 캐시 및 DB 저장 검증
        verify(valueOperations, times(1)).set(eq(cacheKey), eq(updatedProduct));
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), eq(expectedMessage));
        verify(productRepository, times(1)).save(any(Product.class));
    }


    @Test
    void deleteProductTest() {
        // Given
        String productId = "P001";
        String cacheKey = PRODUCT_KEY_PREFIX + productId;
        Product mockProduct = new Product(1L, productId, "가방", 10000L, "실용성 높은 백팩.");

        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(mockProduct));
        doNothing().when(productRepository).delete(mockProduct);

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository, times(1)).delete(mockProduct);
        verify(localCache, times(1)).invalidate(cacheKey);
        verify(redisTemplate, times(1)).delete(cacheKey);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), contains("Deleted product-product:"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Deleted product-product:"));
    }
}
