package SoloTech.StockFlow.stock.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String productId) {
        super("Insufficient stock for product: " + productId);
    }
}
