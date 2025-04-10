package SoloTech.StockFlow.product.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
