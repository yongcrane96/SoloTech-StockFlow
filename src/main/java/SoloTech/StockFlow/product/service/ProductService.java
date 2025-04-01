package SoloTech.StockFlow.product.service;

import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.repository.ProductRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ObjectMapper mapper;

    @Transactional
    public Product createProduct(ProductDto dto) {
        Product product = mapper.convertValue(dto, Product.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        product.setProductId(String.valueOf(snowflakeId));
        return productRepository.saveAndFlush(product);

    }

    public Product getProduct(String productId) {
        return productRepository.findByProductId(productId)
            .orElseThrow(()-> new RuntimeException("Product not found : " + productId));
    }

    @Transactional
    public Product updateProduct(String productId, ProductDto dto) throws JsonMappingException {
        Product product = this.getProduct(productId);
        mapper.updateValue(product, dto);
        return productRepository.save(product);
    }

    public void deleteProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(()-> new RuntimeException("Product not found : " + productId));
        productRepository.delete(product);
    }
}
