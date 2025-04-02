package SoloTech.StockFlow.payment.controller;

import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 컨트롤러
 *
 * @since   2025-04-02
 * @author  yhkim
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // C - R - U - D 형태로 작성

    @PostMapping
    @Operation(summary = "결제 생성", description = "결제를 생성하고 생성된 결제 객체를 반환합니다.")
    public Payment createPayment(@RequestBody PaymentDto dto) {
        return paymentService.createPayment(dto);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 조회", description = "결제 정보를 1건 조회합니다.")
    public Payment readPayment(@PathVariable String paymentId) {
        return paymentService.readPayment(paymentId);
    }

    @PutMapping("/{paymentId}")
    @Operation(summary = "결제 수정", description = "결제 정보를 수정합니다.")
    public Payment updatePayment(@PathVariable String paymentId, @RequestBody PaymentDto dto) throws JsonMappingException {
        return paymentService.updatePayment(paymentId, dto);
    }

    @DeleteMapping("/{paymentId}")
    @Operation(summary = "결제 삭제", description = "결제 정보를 삭제합니다.")
    public boolean deletePayment(@PathVariable String paymentId){
        paymentService.deletePayment(paymentId);
        return true;
    }
}
