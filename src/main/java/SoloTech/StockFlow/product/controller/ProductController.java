package SoloTech.StockFlow.product.controller;


import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.service.ProductService;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 컨트롤러
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping
    @Operation(summary = "상품 등록", description = "상픔을 생성하고 생성된 상품 객체를 반환합니다.")
    public Product createProduct(@RequestBody ProductDto dto){
        return productService.createProduct(dto);
    }

    // 상품 조회
    @GetMapping("{productId}")
    @Operation(summary = "상품 조회", description = "상품 정보를 1건 조회합니다.")
    public ResponseEntity<Product> getProduct(@PathVariable String productId){
        try{
            Product product = productService.getProduct(productId);
            return ResponseEntity.ok(product);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);  // 404 Not Found
        }
    }

    // 상품 수정
    @PutMapping("{productId}")
    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    public Product updateProduct(@PathVariable String productId, @RequestBody ProductDto dto) throws JsonMappingException {
        return productService.updateProduct(productId, dto);
    }

    // 상품 삭제
    @DeleteMapping("{productId}")
    @Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.")
    public boolean deleteProduct(@PathVariable String productId){
        productService.deleteProduct(productId);
        return true;
    }
}
