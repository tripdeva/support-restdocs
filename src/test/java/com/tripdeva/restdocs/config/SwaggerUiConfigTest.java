package com.tripdeva.restdocs.config;

import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerUiConfigTest {

    // 테스트용 최소 Configuration — OpenApiProperties 활성화
    @Configuration
    @EnableConfigurationProperties(OpenApiProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner baseRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class, SwaggerUiConfig.class, SwaggerUiDisabledConfig.class);

    // ═══════════════════════════════════════════════════════════════
    // enabled=true (기본값, matchIfMissing)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("프로퍼티 미설정 시 SwaggerUiConfig가 로드된다 (matchIfMissing=true)")
    void defaultEnabled_swaggerUiConfigLoaded() {
        baseRunner.run(context -> {
            assertThat(context).hasSingleBean(SwaggerUiConfig.class);
            assertThat(context).doesNotHaveBean(SwaggerUiDisabledConfig.class);
        });
    }

    @Test
    @DisplayName("enabled=true 명시 시 SwaggerUiConfig가 로드된다")
    void explicitEnabled_swaggerUiConfigLoaded() {
        baseRunner
                .withPropertyValues("restdocs.openapi.swagger.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SwaggerUiConfig.class);
                    assertThat(context).doesNotHaveBean(SwaggerUiDisabledConfig.class);
                });
    }

    @Test
    @DisplayName("enabled=true 시 springdoc.swagger-ui.url 시스템 프로퍼티가 설정된다")
    void enabled_setsSwaggerUiUrlProperty() {
        // 테스트 격리를 위해 기존 프로퍼티 저장/복원
        String prev = System.getProperty("springdoc.swagger-ui.url");
        System.clearProperty("springdoc.swagger-ui.url");
        try {
            baseRunner.run(context -> {
                String url = System.getProperty("springdoc.swagger-ui.url");
                assertThat(url).isEqualTo("/api-spec/openapi3.json");
            });
        } finally {
            if (prev != null) System.setProperty("springdoc.swagger-ui.url", prev);
            else System.clearProperty("springdoc.swagger-ui.url");
        }
    }

    @Test
    @DisplayName("enabled=true 시 springdoc.api-docs.enabled=false가 설정된다")
    void enabled_disablesSpringdocApiDocs() {
        String prev = System.getProperty("springdoc.api-docs.enabled");
        System.clearProperty("springdoc.api-docs.enabled");
        try {
            baseRunner.run(context -> {
                String val = System.getProperty("springdoc.api-docs.enabled");
                assertThat(val).isEqualTo("false");
            });
        } finally {
            if (prev != null) System.setProperty("springdoc.api-docs.enabled", prev);
            else System.clearProperty("springdoc.api-docs.enabled");
        }
    }

    @Test
    @DisplayName("커스텀 outputFileName이 swagger-ui.url에 반영된다")
    void customOutputFileName_reflectedInUrl() {
        String prev = System.getProperty("springdoc.swagger-ui.url");
        System.clearProperty("springdoc.swagger-ui.url");
        try {
            baseRunner
                    .withPropertyValues("restdocs.openapi.output-file-name=my-api.json")
                    .run(context -> {
                        String url = System.getProperty("springdoc.swagger-ui.url");
                        assertThat(url).isEqualTo("/api-spec/my-api.json");
                    });
        } finally {
            if (prev != null) System.setProperty("springdoc.swagger-ui.url", prev);
            else System.clearProperty("springdoc.swagger-ui.url");
        }
    }

    @Test
    @DisplayName("이미 설정된 시스템 프로퍼티는 덮어쓰지 않는다")
    void existingSystemProperty_notOverwritten() {
        System.setProperty("springdoc.swagger-ui.url", "/custom/existing.json");
        try {
            baseRunner.run(context -> {
                String url = System.getProperty("springdoc.swagger-ui.url");
                assertThat(url).isEqualTo("/custom/existing.json");
            });
        } finally {
            System.clearProperty("springdoc.swagger-ui.url");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // enabled=false
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("enabled=false 시 SwaggerUiDisabledConfig가 로드된다")
    void disabled_swaggerUiDisabledConfigLoaded() {
        baseRunner
                .withPropertyValues("restdocs.openapi.swagger.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(SwaggerUiDisabledConfig.class);
                    assertThat(context).doesNotHaveBean(SwaggerUiConfig.class);
                });
    }

    @Test
    @DisplayName("enabled=false 시 springdoc.swagger-ui.enabled=false가 설정된다")
    void disabled_setsSwaggerUiEnabledFalse() {
        String prev = System.getProperty("springdoc.swagger-ui.enabled");
        System.clearProperty("springdoc.swagger-ui.enabled");
        try {
            baseRunner
                    .withPropertyValues("restdocs.openapi.swagger.enabled=false")
                    .run(context -> {
                        String val = System.getProperty("springdoc.swagger-ui.enabled");
                        assertThat(val).isEqualTo("false");
                    });
        } finally {
            if (prev != null) System.setProperty("springdoc.swagger-ui.enabled", prev);
            else System.clearProperty("springdoc.swagger-ui.enabled");
        }
    }

    @Test
    @DisplayName("enabled=false 시 springdoc.api-docs.enabled=false가 설정된다")
    void disabled_setsApiDocsEnabledFalse() {
        String prev = System.getProperty("springdoc.api-docs.enabled");
        System.clearProperty("springdoc.api-docs.enabled");
        try {
            baseRunner
                    .withPropertyValues("restdocs.openapi.swagger.enabled=false")
                    .run(context -> {
                        String val = System.getProperty("springdoc.api-docs.enabled");
                        assertThat(val).isEqualTo("false");
                    });
        } finally {
            if (prev != null) System.setProperty("springdoc.api-docs.enabled", prev);
            else System.clearProperty("springdoc.api-docs.enabled");
        }
    }

    @Test
    @DisplayName("enabled=false + 이미 설정된 프로퍼티는 덮어쓰지 않는다")
    void disabled_existingProperty_notOverwritten() {
        System.setProperty("springdoc.swagger-ui.enabled", "true");
        try {
            baseRunner
                    .withPropertyValues("restdocs.openapi.swagger.enabled=false")
                    .run(context -> {
                        String val = System.getProperty("springdoc.swagger-ui.enabled");
                        assertThat(val).isEqualTo("true"); // 덮어쓰지 않음
                    });
        } finally {
            System.clearProperty("springdoc.swagger-ui.enabled");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 프로퍼티 바인딩 통합 테스트
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("application 프로퍼티로 swagger.enabled 바인딩")
    void propertyBinding_swaggerEnabled() {
        baseRunner
                .withPropertyValues("restdocs.openapi.swagger.enabled=false")
                .run(context -> {
                    OpenApiProperties props = context.getBean(OpenApiProperties.class);
                    assertThat(props.isSwaggerEnabled()).isFalse();
                });
    }

    @Test
    @DisplayName("application 프로퍼티로 swagger.output-dir 바인딩")
    void propertyBinding_swaggerOutputDir() {
        baseRunner
                .withPropertyValues("restdocs.openapi.swagger.output-dir=custom/dir")
                .run(context -> {
                    OpenApiProperties props = context.getBean(OpenApiProperties.class);
                    assertThat(props.getSwaggerOutputDir()).isEqualTo("custom/dir");
                });
    }

    @Test
    @DisplayName("application 프로퍼티로 title, version 바인딩")
    void propertyBinding_titleVersion() {
        baseRunner
                .withPropertyValues(
                        "restdocs.title=My Custom API",
                        "restdocs.version=3.0.0"
                )
                .run(context -> {
                    OpenApiProperties props = context.getBean(OpenApiProperties.class);
                    assertThat(props.getTitle()).isEqualTo("My Custom API");
                    assertThat(props.getVersion()).isEqualTo("3.0.0");
                });
    }

    @Test
    @DisplayName("getSwaggerUiUrl은 outputFileName과 조합된다")
    void swaggerUiUrl_derivedFromOutputFileName() {
        baseRunner
                .withPropertyValues("restdocs.openapi.output-file-name=v2-api.json")
                .run(context -> {
                    OpenApiProperties props = context.getBean(OpenApiProperties.class);
                    assertThat(props.getSwaggerUiUrl()).isEqualTo("/api-spec/v2-api.json");
                });
    }

    @Test
    @DisplayName("기본값으로 모든 프로퍼티가 정상 바인딩된다")
    void propertyBinding_allDefaults() {
        baseRunner.run(context -> {
            OpenApiProperties props = context.getBean(OpenApiProperties.class);
            assertThat(props.isSwaggerEnabled()).isTrue();
            assertThat(props.getSwaggerOutputDir()).isEqualTo("build/api-spec");
            assertThat(props.getSwaggerUiUrl()).isEqualTo("/api-spec/openapi3.json");
            assertThat(props.getOutputFileName()).isEqualTo("openapi3.json");
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 커스텀 outputDir 설정 시 리소스 핸들러 경로 확인
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("커스텀 output-dir이 SwaggerUiConfig에서 사용된다")
    void customOutputDir_usedByConfig() {
        baseRunner
                .withPropertyValues("restdocs.openapi.swagger.output-dir=my/custom/path")
                .run(context -> {
                    OpenApiProperties props = context.getBean(OpenApiProperties.class);
                    assertThat(props.getSwaggerOutputDir()).isEqualTo("my/custom/path");
                    // SwaggerUiConfig가 로드되었는지 확인
                    assertThat(context).hasSingleBean(SwaggerUiConfig.class);
                });
    }
}
