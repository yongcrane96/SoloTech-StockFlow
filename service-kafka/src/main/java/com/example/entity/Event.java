package com.example.entity;

import lombok.*;

import java.io.Serializable;

/**
 * Event 객체
 *
 * Kafka 메시지로 보내는 데이터 구조를 정의한 클래스입니다.
 * - type: 메시지의 유형을 나타내는 문자열
 * - payload: 메시지의 실제 내용을 담고 있는 문자열
 * @since   2025-04-23
 * @author  yhkim
 */
@Setter
@Data
public class Event implements Serializable { // Serializable을 구현함으로써 바이트 스트림으로 변환 가능
    private int id;
    private String type;
    private String payload;
    private String productId;
    private long quantity;
    private String stockId;
    private String paymentId;
    private String orderId;

    public Event() {}

    /**
     * 생성자: type과 payload를 초기화하는 생성자
     *
     * @param type     메시지 유형
     * @param payload  메시지 내용
     */
    public Event(int id, String type, String payload, String productId, long quantity, String stockId, String paymentId, String orderId){
        this.id = id;
        this.type = type;
        this.payload = payload;
        this.productId = productId;
        this.quantity = quantity;
        this.stockId = stockId;
        this.paymentId = paymentId;
        this.orderId = orderId;
    }
}
