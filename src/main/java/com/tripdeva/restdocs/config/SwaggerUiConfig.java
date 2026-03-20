package com.tripdeva.restdocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger UI 활성화 설정.
 *
 * restdocs.openapi.swagger.enabled=true (기본) 일 때만 활성화.
 * REST Docs로 생성된 openapi3.json을 Swagger UI에서 보여준다.
 *
 * <pre>
 * # application.yml
 * restdocs:
 *   openapi:
 *     swagger:
 *       enabled: true              # false → Swagger UI 비활성화
 *       output-dir: build/api-spec # openapi3 task가 JSON을 생성하는 경로
 * </pre>
 *
 * 흐름:
 *   1. Gradle openapi3 task → build/api-spec/openapi3.json 파일 생성
 *   2. 이 Config가 build/api-spec/ → /api-spec/** URL로 서빙
 *   3. springdoc Swagger UI가 /api-spec/openapi3.json을 로드
 *
 * 접속: http://localhost:8080/swagger-ui/index.html
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "restdocs.openapi.swagger",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SwaggerUiConfig implements WebMvcConfigurer {

    private final OpenApiProperties openApiProperties;

    public SwaggerUiConfig(OpenApiProperties openApiProperties) {
        this.openApiProperties = openApiProperties;

        // springdoc Swagger UI가 REST Docs 생성 JSON을 로드하도록 URL 설정
        String swaggerUiUrl = openApiProperties.getSwaggerUiUrl();
        if (System.getProperty("springdoc.swagger-ui.url") == null) {
            System.setProperty("springdoc.swagger-ui.url", swaggerUiUrl);
        }
        // springdoc 자체 api-docs 스캔 비활성화 (REST Docs JSON만 사용)
        if (System.getProperty("springdoc.api-docs.enabled") == null) {
            System.setProperty("springdoc.api-docs.enabled", "false");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String outputDir = openApiProperties.getSwaggerOutputDir();

        // outputDir (파일 시스템) → /api-spec/** (URL)로 서빙
        registry.addResourceHandler("/api-spec/**")
                .addResourceLocations("file:" + outputDir + "/")
                .setCachePeriod(0);

        log.info("Swagger UI enabled — serving {} → /api-spec/**", outputDir);
        log.info("Swagger UI URL: /swagger-ui/index.html → {}", openApiProperties.getSwaggerUiUrl());
    }
}
