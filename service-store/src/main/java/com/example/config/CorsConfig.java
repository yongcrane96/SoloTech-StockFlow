package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
/**
 * CorsConfig 다른 도메인에서 오는 요청을 서버가 허용할 수 있도록
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@Configuration
public class CorsConfig {
// HTTP 요청에 대해 CORS 설정을 적용하여 다른 도메인에서 오는 요청을 허용 하도록 처리
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 쿠키 사용 허용
        config.addAllowedOrigin("*"); // 클라이언트 모든 도메인에서 오는 요청 허용
        config.addAllowedHeader("*"); // 요청 헤더에 대해서 모든 헤더 허용
        config.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        source.registerCorsConfiguration("/**", config); // 모든 경로에 대해 설정을 적용
        return new CorsFilter(source);
    }
}
