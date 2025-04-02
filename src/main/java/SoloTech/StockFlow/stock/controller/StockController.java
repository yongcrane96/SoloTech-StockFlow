package SoloTech.StockFlow.stock.controller;


import SoloTech.StockFlow.common.controller.BaseRestController;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.service.StockService;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController extends BaseRestController {

    private final StockService stockService;

    @PostMapping
    public Stock createStock(@RequestBody StockDto dto){
        return stockService.createStock(dto);
    }

    @GetMapping("{stockId}")
    public ResponseEntity<?> getStock(@PathVariable String stockId){
        log.info("Get stock by id: {}", stockId);
            Stock stock = stockService.getStock(stockId);

            if(stock == null){
                return getErrorResponse("재고를 찾을 수 없습니다: " + stockId);
            }
            return getOkResponse(stock);
    }

    @PutMapping("{stockId}")
    public Stock updateStock(@PathVariable String stockId, @RequestBody StockDto dto) throws JsonMappingException{
        return stockService.updateStock(stockId,dto);
    }

    // 재고 감소 로직 구성
    @PutMapping("{stockId}/decrease/{quantity}")
    public Stock decreaseStock(@PathVariable String stockId, @PathVariable Long quantity){
        return stockService.decreaseStock(stockId,quantity);
    }

    @DeleteMapping("{stockId}")
    public boolean deleteStock(@PathVariable String stockId){
        stockService.deleteStock(stockId);
        return true;
    }
}
