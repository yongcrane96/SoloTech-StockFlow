package SoloTech.StockFlow.payment.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class PaymentFailedException extends BusinessException {
    public PaymentFailedException(String orderId) {
        super("Payment failed for order: " + orderId);
    }
}