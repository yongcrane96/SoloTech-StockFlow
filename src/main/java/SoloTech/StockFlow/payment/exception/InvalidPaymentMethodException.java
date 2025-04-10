package SoloTech.StockFlow.payment.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class InvalidPaymentMethodException extends BusinessException {
    public InvalidPaymentMethodException(String method) {
        super("Invalid payment method: " + method);
    }
}