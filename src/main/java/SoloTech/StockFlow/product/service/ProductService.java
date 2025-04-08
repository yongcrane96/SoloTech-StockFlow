package SoloTech.StockFlow.product.service;

import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.repository.ProductRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ObjectMapper mapper;


    @Cached(prefix = "product:", key = "#result.productId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Product createProduct(ProductDto dto) {
        Product product = mapper.convertValue(dto, Product.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        product.setProductId(String.valueOf(snowflakeId));
        Product savedProduct = productRepository.saveAndFlush(product);

        return savedProduct;
    }

    @Cached(prefix = "product:", key = "#productId", ttl = 3600, type = CacheType.READ)
    public Product getProduct(String productId) {
        Product dbProduct = productRepository.findByProductId(productId)
            .orElseThrow(()-> new RuntimeException("Product not found : " + productId));

        return dbProduct;
    }

    @Cached(prefix = "product:", key = "#result.productId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Product updateProduct(String productId, ProductDto dto) throws JsonMappingException {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        log.info("Update product by id: {}", productId);

        mapper.updateValue(product, dto);
        Product savedProduct = productRepository.save(product);

        return savedProduct;
    }

    @Cached(prefix = "product:", key = "#productId", ttl = 3600, type = CacheType.DELETE)
    public void deleteProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(()-> new RuntimeException("Product not found : " + productId));
        productRepository.delete(product);
    }
}
