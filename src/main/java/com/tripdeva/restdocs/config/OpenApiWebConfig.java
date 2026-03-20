package com.tripdeva.restdocs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OpenAPI 관련 웹 설정.
 * 리소스 핸들러는 SwaggerUiConfig에서 swagger 활성화 시에만 등록한다.
 */
@Configuration
public class OpenApiWebConfig implements WebMvcConfigurer {
}
