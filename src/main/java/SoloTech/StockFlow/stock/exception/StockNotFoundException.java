package SoloTech.StockFlow.stock.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class StockNotFoundException extends BusinessException {
    public StockNotFoundException(String stockId) {
        super("Stock not found: " + stockId);
    }
}