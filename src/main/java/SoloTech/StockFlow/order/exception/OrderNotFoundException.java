package SoloTech.StockFlow.order.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String orderId){
        super("Order not found: " + orderId);
    }
}
