package com.tripdeva.restdocs.config;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiPropertiesTest {

    private OpenApiProperties props;

    @BeforeEach
    void setUp() {
        props = new OpenApiProperties();
    }

    // ═══════════════════════════════════════════════════════════════
    // 기본값
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("title 기본값")
    void defaultTitle() { assertEquals("API Documentation", props.getTitle()); }

    @Test @DisplayName("version 기본값")
    void defaultVersion() { assertEquals("1.0.0", props.getVersion()); }

    @Test @DisplayName("description 기본값")
    void defaultDescription() { assertEquals("REST Docs 기반으로 생성된 API 문서", props.getDescription()); }

    @Test @DisplayName("outputFileName 기본값")
    void defaultOutputFileName() { assertEquals("openapi3.json", props.getOutputFileName()); }

    @Test @DisplayName("convertRequestBodyToParameters 기본값 true")
    void defaultConvertRequestBody() { assertTrue(props.isConvertRequestBodyToParameters()); }

    @Test @DisplayName("snippetPaths 기본값")
    void defaultSnippetPaths() { assertEquals(List.of("/build/generated-snippets"), props.getSnippetPaths()); }

    @Test @DisplayName("serverHost 기본값")
    void defaultServerHost() { assertEquals("http://localhost", props.getServerHost()); }

    @Test @DisplayName("serverPort 기본값")
    void defaultServerPort() { assertEquals("8080", props.getServerPort()); }

    @Test @DisplayName("contextPath 기본값 빈 문자열")
    void defaultContextPath() { assertEquals("", props.getContextPath()); }

    // ═══════════════════════════════════════════════════════════════
    // setter & getter
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("title setter")
    void setTitle() {
        props.setTitle("My API");
        assertEquals("My API", props.getTitle());
    }

    @Test @DisplayName("version setter")
    void setVersion() {
        props.setVersion("2.0.0");
        assertEquals("2.0.0", props.getVersion());
    }

    @Test @DisplayName("snippetPaths setter")
    void setSnippetPaths() {
        props.getOpenapi().setSnippetPaths(List.of("a", "b"));
        assertEquals(List.of("a", "b"), props.getSnippetPaths());
    }

    @Test @DisplayName("outputFileName setter")
    void setOutputFileName() {
        props.getOpenapi().setOutputFileName("api.json");
        assertEquals("api.json", props.getOutputFileName());
    }

    @Test @DisplayName("convertRequestBodyToParameters setter")
    void setConvertRequestBodyToParameters() {
        props.getOpenapi().setConvertRequestBodyToParameters(false);
        assertFalse(props.isConvertRequestBodyToParameters());
    }

    // ═══════════════════════════════════════════════════════════════
    // getServer — 서버 URL 조합
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("getServer: host:port 조합")
    void getServer_default() {
        assertEquals("http://localhost:8080", props.getServer());
    }

    @Test @DisplayName("getServer: contextPath 포함")
    void getServer_withContextPath() {
        props.getOpenapi().getServer().setContextPath("/api/v1");
        assertEquals("http://localhost:8080/api/v1", props.getServer());
    }

    @Test @DisplayName("getServer: contextPath 빈 문자열이면 미포함")
    void getServer_emptyContextPath() {
        props.getOpenapi().getServer().setContextPath("");
        assertEquals("http://localhost:8080", props.getServer());
    }

    @Test @DisplayName("getServer: contextPath null이면 미포함")
    void getServer_nullContextPath() {
        props.getOpenapi().getServer().setContextPath(null);
        assertEquals("http://localhost:8080", props.getServer());
    }

    @Test @DisplayName("getServer: 커스텀 host와 port")
    void getServer_custom() {
        props.getOpenapi().getServer().setHost("https://api.example.com");
        props.getOpenapi().getServer().setPort("443");
        assertEquals("https://api.example.com:443", props.getServer());
    }

    // ═══════════════════════════════════════════════════════════════
    // OpenApi 내부 클래스
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("OpenApi 객체 접근")
    void openApiObject() {
        assertNotNull(props.getOpenapi());
        assertNotNull(props.getOpenapi().getServer());
    }

    @Test @DisplayName("Server setter/getter")
    void serverSetterGetter() {
        var server = new OpenApiProperties.Server();
        server.setHost("https://test.com");
        server.setPort("9090");
        server.setContextPath("/v2");
        assertEquals("https://test.com", server.getHost());
        assertEquals("9090", server.getPort());
        assertEquals("/v2", server.getContextPath());
    }

    // ═══════════════════════════════════════════════════════════════
    // Swagger 설정
    // ═══════════════════════════════════════════════════════════════

    @Test @DisplayName("swagger.enabled 기본값 true")
    void defaultSwaggerEnabled() {
        assertTrue(props.isSwaggerEnabled());
    }

    @Test @DisplayName("swagger.enabled false 설정")
    void setSwaggerEnabledFalse() {
        props.getOpenapi().getSwagger().setEnabled(false);
        assertFalse(props.isSwaggerEnabled());
    }

    @Test @DisplayName("swagger.outputDir 기본값")
    void defaultSwaggerOutputDir() {
        assertEquals("build/api-spec", props.getSwaggerOutputDir());
    }

    @Test @DisplayName("swagger.outputDir 커스텀 설정")
    void setSwaggerOutputDir() {
        props.getOpenapi().getSwagger().setOutputDir("custom/output");
        assertEquals("custom/output", props.getSwaggerOutputDir());
    }

    @Test @DisplayName("getSwaggerUiUrl: outputFileName과 조합")
    void swaggerUiUrl_default() {
        assertEquals("/api-spec/openapi3.json", props.getSwaggerUiUrl());
    }

    @Test @DisplayName("getSwaggerUiUrl: 커스텀 outputFileName 반영")
    void swaggerUiUrl_customFileName() {
        props.getOpenapi().setOutputFileName("custom-api.json");
        assertEquals("/api-spec/custom-api.json", props.getSwaggerUiUrl());
    }

    @Test @DisplayName("Swagger 객체 접근")
    void swaggerObject() {
        assertNotNull(props.getOpenapi().getSwagger());
    }

    @Test @DisplayName("Swagger setter/getter")
    void swaggerSetterGetter() {
        var swagger = new OpenApiProperties.Swagger();
        swagger.setEnabled(false);
        swagger.setOutputDir("my/dir");
        assertFalse(swagger.isEnabled());
        assertEquals("my/dir", swagger.getOutputDir());
    }
}
