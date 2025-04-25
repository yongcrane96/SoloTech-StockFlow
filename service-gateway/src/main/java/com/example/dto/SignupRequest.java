package com.example.dto;

import lombok.Data;

// 요청 및 응답 DTO
@Data
public class SignupRequest {
    private String username;
    private String password;
}