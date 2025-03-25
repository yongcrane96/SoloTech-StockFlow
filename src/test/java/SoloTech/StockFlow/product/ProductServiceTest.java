package SoloTech.StockFlow.product;

import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.repository.ProductRepository;
import SoloTech.StockFlow.product.service.ProductService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화
    }

    @Test
    void getProductTest(){
        Product mockProduct = new Product(1L, "P001", "가방", 10000L, "실용성 높은 백팩.");
        Mockito.when(productRepository.findByProductId(any(String.class))).thenReturn(java.util.Optional.of(mockProduct));

        Product result = productService.getProduct("P001");

        assertNotNull(result);
        assertEquals("P001", result.getProductId());
        assertEquals("가방", result.getName());
    }

    @Test
    void updateProductTest() throws JsonMappingException {
        // 준비 단계 (Arrange) updateDto : 상품을 업데이트할 정보를 담고 있다.
        Product mockProduct = new Product(1L, "P001", "가방", 10000L, "실용성 높은 백팩.");
        ProductDto updateDto = new ProductDto("수정된 가방", 12000L, "더 실용적이고 멋진 백팩");

        // 기존 Product 반환 설정
        Mockito.when(productRepository.findByProductId("P001")).thenReturn(java.util.Optional.of(mockProduct));

        // 저장된 Product 반환 설정
        Product updatedProduct = new Product(1L, "P001", "수정된 가방", 12000L, "더 실용적이고 멋진 백팩.");
        Mockito.when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // 테스트 실행 (Act)
        Product result = productService.updateProduct("P001", updateDto);

        // 검증 단계 (Assert)
        assertNotNull(result);
        assertEquals("수정된 가방", result.getName());
        assertEquals(12000L, result.getPrice());
        assertEquals("더 실용적이고 멋진 백팩.", result.getContent());

        // Mock 동작 확인
        Mockito.verify(productRepository).findByProductId("P001");
        Mockito.verify(productRepository).save(mockProduct);
    }
}
