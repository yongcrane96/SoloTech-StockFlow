package com.example.payment;


import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFeignClient paymentFeignClient;

    @PostMapping
    public String createPayment(@RequestBody PaymentRequest request) {
        log.info("Create payment request: {}", request);
        return paymentFeignClient.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable String paymentId) {
        log.info("Get payment by id {}", paymentId);
        return paymentFeignClient.getPayment(paymentId);
    }

    @PostMapping("/{paymentId}")
    public boolean updatePayment(
            @PathVariable String paymentId,
            @RequestBody PaymentRequest request
    ){
        return paymentFeignClient.updatePayment(paymentId, request);
    }

    @DeleteMapping("/{paymentId}")
    public boolean deletePayment(@PathVariable String paymentId)
    {
        return paymentFeignClient.deletePayment(paymentId);
    }
}
