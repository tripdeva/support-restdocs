package com.tripdeva.restdocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 비활성화 설정.
 *
 * restdocs.openapi.swagger.enabled=false 일 때 활성화.
 * springdoc의 Swagger UI와 /v3/api-docs 엔드포인트를 모두 비활성화한다.
 *
 * Spring Boot에서 springdoc 비활성화는 application.yml에 아래 프로퍼티를 추가하면 되지만,
 * 이 Config가 로드되면 자동으로 시스템 프로퍼티로 주입하여 사용자가 별도 설정 없이도 동작하게 한다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "restdocs.openapi.swagger",
        name = "enabled",
        havingValue = "false"
)
public class SwaggerUiDisabledConfig {

    public SwaggerUiDisabledConfig() {
        // springdoc 비활성화 — 시스템 프로퍼티가 없을 때만 설정
        if (System.getProperty("springdoc.swagger-ui.enabled") == null) {
            System.setProperty("springdoc.swagger-ui.enabled", "false");
        }
        if (System.getProperty("springdoc.api-docs.enabled") == null) {
            System.setProperty("springdoc.api-docs.enabled", "false");
        }
        log.info("Swagger UI disabled by restdocs.openapi.swagger.enabled=false");
    }
}
