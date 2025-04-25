package com.example.payment.dto;

public enum PaymentStatus {
    PENDING,   // 결제 대기
    PAID,      // 결제 완료
    CANCELED,   // 결제 취소
    SUCCESS
}
