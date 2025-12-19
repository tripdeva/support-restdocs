package com.tripdeva.restdocs.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * OpenAPI 3.0 생성을 위한 설정
 * application.yml의 기존 키들을 활용
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "restdocs")
public class OpenApiProperties {

    /**
     * API 제목 (restdocs.title)
     */
    private String title = "API Documentation";

    /**
     * API 버전 (restdocs.version)
     */
    private String version = "1.0.0";
    
    /**
     * OpenAPI 관련 설정 (restdocs.openapi)
     */
    private OpenApi openapi = new OpenApi();

    @Getter
    @Setter
    public static class OpenApi {
        /**
         * 서버 설정
         */
        private Server server = new Server();

        /**
         * OpenAPI 출력 파일명
         */
        private String outputFileName = "openapi3.json";

        /**
         * Request Body 필드를 Parameters로 변환할지 여부
         */
        private boolean convertRequestBodyToParameters = true;

        /**
         * REST Docs snippets 디렉토리 경로 목록
         */
        private List<String> snippetPaths = List.of("/build/generated-snippets");
    }

    @Getter
    @Setter
    public static class Server {
        private String host = "http://localhost";
        private String port = "8080";
        private String contextPath = "";
    }

    /**
     * 서버 정보 반환 (기존 호환성을 위한 메서드들)
     */
    public String getServerHost() {
        return openapi.server.host;
    }

    public String getServerPort() {
        return openapi.server.port;
    }

    public String getContextPath() {
        return openapi.server.contextPath;
    }

    /**
     * 완전한 서버 URL 반환
     */
    public String getServer() {
        String baseUrl = openapi.server.host + ":" + openapi.server.port;
        if (openapi.server.contextPath != null && !openapi.server.contextPath.isEmpty()) {
            baseUrl += openapi.server.contextPath;
        }
        return baseUrl;
    }
    
    /**
     * API 설명 (기본값)
     */
    public String getDescription() {
        return "REST Docs 기반으로 생성된 API 문서";
    }
    
    /**
     * OpenAPI 출력 파일명
     */
    public String getOutputFileName() {
        return openapi.outputFileName;
    }
    
    /**
     * Request Body 필드를 Parameters로 변환할지 여부
     */
    public boolean isConvertRequestBodyToParameters() {
        return openapi.convertRequestBodyToParameters;
    }
    
    /**
     * REST Docs snippets 디렉토리 경로 목록
     */
    public List<String> getSnippetPaths() {
        return openapi.snippetPaths;
    }
    
    @PostConstruct
    public void logProperties() {
        log.info("=== OpenAPI Properties Loaded ===");
        log.info("Title: {}", title);
        log.info("Version: {}", version);
        log.info("Server Host: {}", openapi.server.host);
        log.info("Server Port: {}", openapi.server.port);
        log.info("Context Path: {}", openapi.server.contextPath);
        log.info("Full Server URL: {}", getServer());
        log.info("================================");
    }
}