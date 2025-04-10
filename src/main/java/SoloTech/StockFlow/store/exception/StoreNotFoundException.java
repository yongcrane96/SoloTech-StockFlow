package SoloTech.StockFlow.store.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class StoreNotFoundException extends BusinessException {
    public StoreNotFoundException(String storeId) {
        super("Store not found: " + storeId);
    }
}
