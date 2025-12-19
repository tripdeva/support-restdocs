package com.tripdeva.restdocs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OpenAPI 관련 웹 설정
 * - build/api-spec 디렉토리를 웹에서 접근 가능하도록 ResourceHandler 등록
 */
@Configuration
public class OpenApiWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /api-spec/** 경로로 접근할 수 있도록 설정
        registry.addResourceHandler("/api-spec/**")
                .addResourceLocations("file:build/api-spec/")
                .setCachePeriod(0); // 개발시 캐시 비활성화
    }
}