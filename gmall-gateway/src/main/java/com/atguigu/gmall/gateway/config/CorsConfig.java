package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        // cors配置对象
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:1000"); // 允许跨域请求的路径
        config.addAllowedOrigin("http://127.0.0.1:1000"); // 允许跨域请求的路径
        config.addAllowedHeader("*"); // 允许携带的头信息
        config.setAllowCredentials(true); // 是否允许携带cookie
        config.addAllowedMethod("*"); // 允许那些请求方法跨域访问

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(configurationSource);
    }
}
