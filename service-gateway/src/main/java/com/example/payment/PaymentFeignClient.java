package com.example.payment;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "paymentClient", url = "http://localhost:8088")
public interface PaymentFeignClient {

    @PostMapping("/api/payment")
    String createPayment(@RequestBody PaymentRequest request);

    @GetMapping("/api/payment/{paymentId}")
    PaymentResponse getPayment(@PathVariable("paymentId") String paymentId);

    @PutMapping("/api/payment/{paymentId}")
    boolean updatePayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody PaymentRequest request
    );

    @DeleteMapping("/api/payment/{payment}")
    boolean deletePayment(@PathVariable("paymentId") String paymentId);


}
