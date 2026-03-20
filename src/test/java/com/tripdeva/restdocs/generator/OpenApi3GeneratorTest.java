package com.tripdeva.restdocs.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripdeva.restdocs.config.OpenApiProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApi3GeneratorTest {

    private OpenApi3Generator generator;
    private OpenApiProperties properties;
    private ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new OpenApiProperties();
        properties.setTitle("Test API");
        properties.setVersion("2.0.0");
        generator = new OpenApi3Generator(properties);
    }

    // ═══════════════════════════════════════════════════════════════
    // generateOpenApiJson — 전체 흐름
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generateOpenApiJson: 기본 구조가 올바르게 생성된다")
    void generateOpenApiJson_createsValidStructure() throws Exception {
        Path snippets = tempDir.resolve("snippets");
        Files.createDirectories(snippets);
        Path output = tempDir.resolve("output");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        File json = new File(output.toFile(), "openapi3.json");
        assertTrue(json.exists());
        JsonNode root = objectMapper.readTree(json);
        assertEquals("3.0.0", root.get("openapi").asText());
        assertEquals("Test API", root.get("info").get("title").asText());
        assertEquals("2.0.0", root.get("info").get("version").asText());
        assertNotNull(root.get("servers"));
        assertNotNull(root.get("paths"));
        assertNotNull(root.get("components"));
    }

    @Test
    @DisplayName("generateOpenApiJson: 출력 디렉토리가 없으면 자동 생성된다")
    void generateOpenApiJson_createsOutputDir() throws Exception {
        Path snippets = tempDir.resolve("snippets");
        Files.createDirectories(snippets);
        Path output = tempDir.resolve("deep/nested/output");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        assertTrue(output.toFile().exists());
    }

    @Test
    @DisplayName("generateOpenApiJson: 존재하지 않는 snippet 경로는 건너뛴다")
    void generateOpenApiJson_skipsNonExistentSnippetPath() throws Exception {
        Path output = tempDir.resolve("output");
        properties.getOpenapi().setSnippetPaths(List.of("/nonexistent/path"));

        assertDoesNotThrow(() -> generator.generateOpenApiJson(null, output.toAbsolutePath().toString()));
    }

    @Test
    @DisplayName("generateOpenApiJson: 절대 경로 snippet 처리")
    void generateOpenApiJson_absoluteSnippetPath() throws Exception {
        Path snippets = tempDir.resolve("abs-snippets");
        Files.createDirectories(snippets);
        createMinimalOperation(snippets, "test-op", "GET", "/api/test");
        Path output = tempDir.resolve("output");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/test"));
    }

    @Test
    @DisplayName("generateOpenApiJson: 여러 snippet 경로 처리")
    void generateOpenApiJson_multipleSnippetPaths() throws Exception {
        Path snippets1 = tempDir.resolve("snippets1");
        Path snippets2 = tempDir.resolve("snippets2");
        Files.createDirectories(snippets1);
        Files.createDirectories(snippets2);
        createMinimalOperation(snippets1, "op1", "GET", "/api/one");
        createMinimalOperation(snippets2, "op2", "POST", "/api/two");
        Path output = tempDir.resolve("output");
        properties.getOpenapi().setSnippetPaths(List.of(
                snippets1.toAbsolutePath().toString(),
                snippets2.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/one"));
        assertNotNull(root.get("paths").get("/api/two"));
    }

    @Test
    @DisplayName("generateOpenApiJson: 빈 snippet 디렉토리")
    void generateOpenApiJson_emptySnippetsDir() throws Exception {
        Path snippets = tempDir.resolve("empty-snippets");
        Files.createDirectories(snippets);
        Path output = tempDir.resolve("output");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals(0, root.get("paths").size());
    }

    // ═══════════════════════════════════════════════════════════════
    // parseHttpRequest — HTTP 요청 파싱
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @CsvSource({
            "GET,    /api/users",
            "POST,   /api/users",
            "PUT,    /api/users/1",
            "DELETE,  /api/users/1",
            "PATCH,  /api/users/1",
            "HEAD,   /api/status",
            "OPTIONS, /api/users"
    })
    @DisplayName("parseHttpRequest: 다양한 HTTP 메서드 파싱")
    void parseHttpRequest_variousMethods(String method, String path) throws Exception {
        Path snippets = tempDir.resolve("snippets-" + method);
        Files.createDirectories(snippets);
        createMinimalOperation(snippets, "op-" + method.toLowerCase(), method, path);
        Path output = tempDir.resolve("output-" + method);
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode pathNode = root.get("paths").get(path);
        assertNotNull(pathNode, "path " + path + " should exist");
        assertNotNull(pathNode.get(method.toLowerCase()), method + " method should exist");
    }

    @Test
    @DisplayName("parseHttpRequest: query string이 path에서 제거된다")
    void parseHttpRequest_removesQueryString() throws Exception {
        Path snippets = tempDir.resolve("snippets-qs");
        Files.createDirectories(snippets);
        Path opDir = snippets.resolve("qs-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", """
                [source,http,options="nowrap"]
                ----
                GET /api/items?page=0&size=20&sort=name HTTP/1.1
                Host: localhost

                ----
                """);
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-qs");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/items"));
        assertNull(root.get("paths").get("/api/items?page=0&size=20&sort=name"));
    }

    @Test
    @DisplayName("parseHttpRequest: http-request.adoc 없으면 operation 건너뜀")
    void parseHttpRequest_missingFile_skipped() throws Exception {
        Path snippets = tempDir.resolve("snippets-noreq");
        Path opDir = snippets.resolve("no-req");
        Files.createDirectories(opDir);
        // http-request.adoc 없이 다른 파일만 생성
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-noreq");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals(0, root.get("paths").size());
    }

    @Test
    @DisplayName("parseHttpRequest: 빈 http-request.adoc는 null 반환")
    void parseHttpRequest_emptyFile() throws Exception {
        Path snippets = tempDir.resolve("snippets-empty-req");
        Path opDir = snippets.resolve("empty-req");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", "");
        Path output = tempDir.resolve("output-empty-req");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals(0, root.get("paths").size());
    }

    @Test
    @DisplayName("parseHttpRequest: source 어노테이션과 구분선 무시")
    void parseHttpRequest_skipsAnnotationsAndDelimiters() throws Exception {
        Path snippets = tempDir.resolve("snippets-anno");
        Path opDir = snippets.resolve("anno-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", """
                [source,http,options="nowrap"]
                ----
                POST /api/data HTTP/1.1
                Host: localhost
                Content-Type: application/json

                {"key":"val"}
                ----
                """);
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-anno");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/data"));
        assertNotNull(root.get("paths").get("/api/data").get("post"));
    }

    // ═══════════════════════════════════════════════════════════════
    // parseFieldType — 타입 매핑
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @CsvSource({
            "String, string",
            "string, string",
            "STRING, string",
            "Integer, integer",
            "integer, integer",
            "Number, number",
            "number, number",
            "Boolean, boolean",
            "boolean, boolean",
            "Array, array",
            "array, array",
            "Object, object",
            "object, object",
            "UnknownType, string"
    })
    @DisplayName("parseFieldType: 타입 매핑 검증")
    void parseFieldType_mapping(String input, String expected) throws Exception {
        // request-fields.adoc에 해당 타입 넣고 검증
        Path snippets = tempDir.resolve("snippets-type-" + input);
        Path opDir = snippets.resolve("type-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/type-test", "{\"field\":\"v\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("field", input, "desc"));
        Path output = tempDir.resolve("output-type-" + input);
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode schema = root.get("components").get("schemas").get("type-opRequest");
        assertEquals(expected, schema.get("properties").get("field").get("type").asText());
    }

    @Test
    @DisplayName("parseFieldType: number와 integer 구분 — integer 우선 매칭")
    void parseFieldType_integerBeforeNumber() throws Exception {
        Path snippets = tempDir.resolve("snippets-int");
        Path opDir = snippets.resolve("int-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/int-test", "{\"a\":1,\"b\":1.5}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", """
                |===
                |Path|Type|Description

                |`+a+`
                |`+Integer+`
                |정수 필드

                |`+b+`
                |`+Number+`
                |실수 필드

                |===
                """);
        Path output = tempDir.resolve("output-int");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode schema = root.get("components").get("schemas").get("int-opRequest");
        assertEquals("integer", schema.get("properties").get("a").get("type").asText());
        assertEquals("number", schema.get("properties").get("b").get("type").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // extractResponseStatusCode — 상태코드 추출
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @CsvSource({
            "200 OK, 200",
            "201 Created, 201",
            "204 No Content, 204",
            "301 Moved Permanently, 301",
            "400 Bad Request, 400",
            "401 Unauthorized, 401",
            "403 Forbidden, 403",
            "404 Not Found, 404",
            "409 Conflict, 409",
            "500 Internal Server Error, 500",
            "502 Bad Gateway, 502",
            "503 Service Unavailable, 503"
    })
    @DisplayName("extractResponseStatusCode: 다양한 HTTP 상태코드")
    void extractResponseStatusCode_variousCodes(String statusLine, String expectedCode) throws Exception {
        Path snippets = tempDir.resolve("snippets-sc-" + expectedCode);
        Path opDir = snippets.resolve("sc-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/sc-" + expectedCode, null));
        writeFile(opDir, "http-response.adoc", httpResponse(statusLine, "{}"));
        Path output = tempDir.resolve("output-sc-" + expectedCode);
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode responses = root.get("paths").get("/api/sc-" + expectedCode).get("get").get("responses");
        assertNotNull(responses.get(expectedCode), "Expected status code " + expectedCode);
    }

    @Test
    @DisplayName("extractResponseStatusCode: http-response.adoc 없으면 200 기본")
    void extractResponseStatusCode_noFile_defaults200() throws Exception {
        Path snippets = tempDir.resolve("snippets-nosc");
        Path opDir = snippets.resolve("nosc-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/nosc", null));
        // http-response.adoc 없음
        Path output = tempDir.resolve("output-nosc");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/nosc").get("get").get("responses").get("200"));
    }

    @Test
    @DisplayName("extractResponseStatusCode: 잘못된 포맷은 200 기본")
    void extractResponseStatusCode_invalidFormat_defaults200() throws Exception {
        Path snippets = tempDir.resolve("snippets-badsc");
        Path opDir = snippets.resolve("badsc-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/badsc", null));
        writeFile(opDir, "http-response.adoc", "[source,http]\n----\nINVALID RESPONSE\n----\n");
        Path output = tempDir.resolve("output-badsc");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/badsc").get("get").get("responses").get("200"));
    }

    // ═══════════════════════════════════════════════════════════════
    // templatizePath — 경로 템플릿화
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("templatizePath: 숫자 ID를 path param으로 치환")
    void templatizePath_numericId() throws Exception {
        Path snippets = tempDir.resolve("snippets-tpl1");
        Path opDir = snippets.resolve("tpl1-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/users/42", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("id", "사용자 ID"));
        Path output = tempDir.resolve("output-tpl1");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/users/{id}"));
    }

    @Test
    @DisplayName("templatizePath: UUID를 path param으로 치환")
    void templatizePath_uuid() throws Exception {
        Path snippets = tempDir.resolve("snippets-tpl2");
        Path opDir = snippets.resolve("tpl2-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/orders/a1b2c3d4-e5f6-7890-abcd-ef1234567890", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("orderId", "주문 ID"));
        Path output = tempDir.resolve("output-tpl2");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/orders/{orderId}"));
    }

    @Test
    @DisplayName("templatizePath: 다중 path params 치환")
    void templatizePath_multipleParams() throws Exception {
        Path snippets = tempDir.resolve("snippets-tpl3");
        Path opDir = snippets.resolve("tpl3-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/users/1/orders/99", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", """
                |===
                |Name|Description

                |`+userId+`
                |사용자 ID

                |`+orderId+`
                |주문 ID

                |===
                """);
        Path output = tempDir.resolve("output-tpl3");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/users/{userId}/orders/{orderId}"));
    }

    @Test
    @DisplayName("templatizePath: path-parameters.adoc 없으면 원본 유지")
    void templatizePath_noPathParams_keepOriginal() throws Exception {
        Path snippets = tempDir.resolve("snippets-tpl4");
        Path opDir = snippets.resolve("tpl4-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/health", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-tpl4");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/health"));
    }

    @Test
    @DisplayName("templatizePath: 문자열 세그먼트는 치환하지 않음")
    void templatizePath_nonNumericSegment_notReplaced() throws Exception {
        Path snippets = tempDir.resolve("snippets-tpl5");
        Path opDir = snippets.resolve("tpl5-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/users/admin/profile", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("username", "사용자명"));
        Path output = tempDir.resolve("output-tpl5");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        // "admin"은 숫자가 아니므로 치환 안 됨
        assertNotNull(root.get("paths").get("/api/users/admin/profile"));
    }

    // ═══════════════════════════════════════════════════════════════
    // parseParameterAdoc — 파라미터 파싱
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addQueryParameters: query-parameters.adoc 우선, fallback request-parameters.adoc")
    void addQueryParameters_fallbackToRequestParams() throws Exception {
        Path snippets = tempDir.resolve("snippets-qp");
        Path opDir = snippets.resolve("qp-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/fallback", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        // query-parameters.adoc 없고 request-parameters.adoc만 있음
        writeFile(opDir, "request-parameters.adoc", """
                |===
                |Parameter|Description

                |`+keyword+`
                |검색어

                |===
                """);
        Path output = tempDir.resolve("output-qp");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/fallback").get("get").get("parameters");
        assertNotNull(params);
        assertEquals("keyword", params.get(0).get("name").asText());
        assertEquals("query", params.get(0).get("in").asText());
    }

    @Test
    @DisplayName("addQueryParameters: 다수 파라미터 파싱")
    void addQueryParameters_multipleParams() throws Exception {
        Path snippets = tempDir.resolve("snippets-mqp");
        Path opDir = snippets.resolve("mqp-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/search", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "query-parameters.adoc", """
                |===
                |Parameter|Description

                |`+q+`
                |검색어

                |`+page+`
                |페이지

                |`+size+`
                |크기

                |`+sort+`
                |정렬

                |===
                """);
        Path output = tempDir.resolve("output-mqp");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/search").get("get").get("parameters");
        assertEquals(4, params.size());
    }

    @Test
    @DisplayName("addQueryParameters: required는 false")
    void addQueryParameters_requiredIsFalse() throws Exception {
        Path snippets = tempDir.resolve("snippets-qpreq");
        Path opDir = snippets.resolve("qpreq-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/qp-req", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "query-parameters.adoc", queryParamsAdoc("filter", "필터"));
        Path output = tempDir.resolve("output-qpreq");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode param = root.get("paths").get("/api/qp-req").get("get").get("parameters").get(0);
        assertFalse(param.get("required").asBoolean());
    }

    @Test
    @DisplayName("addPathParameters: required는 true")
    void addPathParameters_requiredIsTrue() throws Exception {
        Path snippets = tempDir.resolve("snippets-ppreq");
        Path opDir = snippets.resolve("ppreq-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/items/1", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("id", "항목 ID"));
        Path output = tempDir.resolve("output-ppreq");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/items/{id}").get("get").get("parameters");
        assertTrue(params.get(0).get("required").asBoolean());
        assertEquals("path", params.get(0).get("in").asText());
    }

    @Test
    @DisplayName("addQueryParameters: 파일 없으면 파라미터 없음")
    void addQueryParameters_noFile_noParams() throws Exception {
        Path snippets = tempDir.resolve("snippets-noqp");
        Path opDir = snippets.resolve("noqp-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/no-params", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-noqp");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/no-params").get("get").get("parameters"));
    }

    @Test
    @DisplayName("parseParameterAdoc: 헤더 행 무시")
    void parseParameterAdoc_ignoresHeaders() throws Exception {
        Path snippets = tempDir.resolve("snippets-hdr");
        Path opDir = snippets.resolve("hdr-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/hdr", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "query-parameters.adoc", """
                |===
                |Parameter|Description

                |`+name+`
                |이름

                |===
                """);
        Path output = tempDir.resolve("output-hdr");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/hdr").get("get").get("parameters");
        assertEquals(1, params.size());
        assertEquals("name", params.get(0).get("name").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // addRequestHeaders — 요청 헤더
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addRequestHeaders: request-headers.adoc 파싱")
    void addRequestHeaders_parsesCorrectly() throws Exception {
        Path snippets = tempDir.resolve("snippets-rh");
        Path opDir = snippets.resolve("rh-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/rh", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-headers.adoc", """
                |===
                |Name|Description

                |`+Authorization+`
                |인증 토큰

                |`+X-Custom+`
                |커스텀 헤더

                |===
                """);
        Path output = tempDir.resolve("output-rh");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/rh").get("get").get("parameters");
        assertEquals(2, params.size());
        assertEquals("header", params.get(0).get("in").asText());
        assertEquals("Authorization", params.get(0).get("name").asText());
    }

    @Test
    @DisplayName("addRequestHeaders: 파일 없으면 무시")
    void addRequestHeaders_noFile_noHeaders() throws Exception {
        Path snippets = tempDir.resolve("snippets-norh");
        Path opDir = snippets.resolve("norh-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/norh", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-norh");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/norh").get("get").get("parameters"));
    }

    // ═══════════════════════════════════════════════════════════════
    // parseResponseHeaders — 응답 헤더
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("parseResponseHeaders: response-headers.adoc 파싱")
    void parseResponseHeaders_parsesCorrectly() throws Exception {
        Path snippets = tempDir.resolve("snippets-resph");
        Path opDir = snippets.resolve("resph-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/resph", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "response-headers.adoc", """
                |===
                |Name|Description

                |`+X-Request-Id+`
                |요청 ID

                |`+X-Rate-Limit+`
                |속도 제한

                |===
                """);
        Path output = tempDir.resolve("output-resph");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode headers = root.get("paths").get("/api/resph").get("get").get("responses").get("200").get("headers");
        assertNotNull(headers);
        assertEquals("요청 ID", headers.get("X-Request-Id").get("description").asText());
        assertEquals("속도 제한", headers.get("X-Rate-Limit").get("description").asText());
    }

    @Test
    @DisplayName("parseResponseHeaders: 파일 없으면 headers 없음")
    void parseResponseHeaders_noFile_noHeaders() throws Exception {
        Path snippets = tempDir.resolve("snippets-noresph");
        Path opDir = snippets.resolve("noresph-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/noresph", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-noresph");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/noresph").get("get").get("responses").get("200").get("headers"));
    }

    // ═══════════════════════════════════════════════════════════════
    // createRequestBody — 요청 본문
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createRequestBody: POST에서 request body 생성")
    void createRequestBody_postMethod() throws Exception {
        Path snippets = tempDir.resolve("snippets-rb");
        Path opDir = snippets.resolve("rb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/rb", "{\"name\":\"test\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("201 Created", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("name", "String", "이름"));
        Path output = tempDir.resolve("output-rb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode rb = root.get("paths").get("/api/rb").get("post").get("requestBody");
        assertNotNull(rb);
        assertTrue(rb.get("required").asBoolean());
        assertNotNull(rb.get("content").get("application/json").get("schema").get("$ref"));
    }

    @Test
    @DisplayName("createRequestBody: GET에서는 request body 없음")
    void createRequestBody_getMethod_noBody() throws Exception {
        Path snippets = tempDir.resolve("snippets-norb");
        Path opDir = snippets.resolve("norb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/norb", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("name", "String", "이름"));
        Path output = tempDir.resolve("output-norb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/norb").get("get").get("requestBody"));
    }

    @Test
    @DisplayName("createRequestBody: PUT에서 request body 생성")
    void createRequestBody_putMethod() throws Exception {
        Path snippets = tempDir.resolve("snippets-put-rb");
        Path opDir = snippets.resolve("put-rb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("PUT", "/api/put-rb", "{\"name\":\"updated\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("name", "String", "이름"));
        Path output = tempDir.resolve("output-put-rb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/put-rb").get("put").get("requestBody"));
    }

    @Test
    @DisplayName("createRequestBody: PATCH에서 request body 생성")
    void createRequestBody_patchMethod() throws Exception {
        Path snippets = tempDir.resolve("snippets-patch-rb");
        Path opDir = snippets.resolve("patch-rb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("PATCH", "/api/patch-rb", "{\"name\":\"patched\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("name", "String", "이름"));
        Path output = tempDir.resolve("output-patch-rb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/patch-rb").get("patch").get("requestBody"));
    }

    @Test
    @DisplayName("createRequestBody: DELETE에서는 request body 없음")
    void createRequestBody_deleteMethod_noBody() throws Exception {
        Path snippets = tempDir.resolve("snippets-del-rb");
        Path opDir = snippets.resolve("del-rb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("DELETE", "/api/del-rb", null));
        writeFile(opDir, "http-response.adoc", httpResponse("204 No Content", ""));
        Path output = tempDir.resolve("output-del-rb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/del-rb").get("delete").get("requestBody"));
    }

    @Test
    @DisplayName("createRequestBody: optional 필드는 required 배열에 미포함")
    void createRequestBody_optionalFields_notInRequired() throws Exception {
        Path snippets = tempDir.resolve("snippets-opt");
        Path opDir = snippets.resolve("opt-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/opt", "{\"a\":\"v\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", """
                |===
                |Path|Type|Description

                |`+name+`
                |`+String+`
                |이름

                |`+nickname+`
                |`+String (optional)+`
                |별명

                |===
                """);
        Path output = tempDir.resolve("output-opt");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode schema = root.get("components").get("schemas").get("opt-opRequest");
        ArrayNode required = (ArrayNode) schema.get("required");
        assertEquals(1, required.size());
        assertEquals("name", required.get(0).asText());
    }

    @Test
    @DisplayName("createRequestBody: 모든 필드 optional이면 required 배열 없음")
    void createRequestBody_allOptional_noRequired() throws Exception {
        Path snippets = tempDir.resolve("snippets-allopt");
        Path opDir = snippets.resolve("allopt-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("PATCH", "/api/allopt", "{\"a\":\"v\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", """
                |===
                |Path|Type|Description

                |`+name+`
                |`+String (optional)+`
                |이름

                |`+email+`
                |`+String (optional)+`
                |이메일

                |===
                """);
        Path output = tempDir.resolve("output-allopt");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode schema = root.get("components").get("schemas").get("allopt-opRequest");
        assertNull(schema.get("required"));
    }

    @Test
    @DisplayName("createRequestBody: request-fields.adoc 없으면 null")
    void createRequestBody_noFieldsFile_null() throws Exception {
        Path snippets = tempDir.resolve("snippets-norf");
        Path opDir = snippets.resolve("norf-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/norf", "{\"a\":\"v\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-norf");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNull(root.get("paths").get("/api/norf").get("post").get("requestBody"));
    }

    @Test
    @DisplayName("createRequestBody: 다수 필드 처리")
    void createRequestBody_multipleFields() throws Exception {
        Path snippets = tempDir.resolve("snippets-mf");
        Path opDir = snippets.resolve("mf-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/mf", "{\"a\":1}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", """
                |===
                |Path|Type|Description

                |`+name+`
                |`+String+`
                |이름

                |`+age+`
                |`+Number+`
                |나이

                |`+active+`
                |`+Boolean+`
                |활성 여부

                |`+tags+`
                |`+Array+`
                |태그 목록

                |`+address+`
                |`+Object+`
                |주소

                |===
                """);
        Path output = tempDir.resolve("output-mf");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode props = root.get("components").get("schemas").get("mf-opRequest").get("properties");
        assertEquals(5, props.size());
        assertEquals("string", props.get("name").get("type").asText());
        assertEquals("number", props.get("age").get("type").asText());
        assertEquals("boolean", props.get("active").get("type").asText());
        assertEquals("array", props.get("tags").get("type").asText());
        assertEquals("object", props.get("address").get("type").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // extractExampleFromHttpRequest — example 추출
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("extractExample: 멀티라인 pretty-printed JSON 파싱")
    void extractExample_multilinePrettyPrinted() throws Exception {
        Path snippets = tempDir.resolve("snippets-mlj");
        Path opDir = snippets.resolve("mlj-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", """
                [source,http,options="nowrap"]
                ----
                POST /api/mlj HTTP/1.1
                Content-Type: application/json
                Host: localhost

                {
                  "name" : "John",
                  "age" : 30
                }
                ----
                """);
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", """
                |===
                |Path|Type|Description

                |`+name+`
                |`+String+`
                |이름

                |`+age+`
                |`+Number+`
                |나이

                |===
                """);
        Path output = tempDir.resolve("output-mlj");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode example = root.get("paths").get("/api/mlj").get("post")
                .get("requestBody").get("content").get("application/json").get("example");
        assertNotNull(example);
        assertEquals("John", example.get("name").asText());
        assertEquals(30, example.get("age").asInt());
    }

    @Test
    @DisplayName("extractExample: 한 줄 JSON 파싱")
    void extractExample_singleLineJson() throws Exception {
        Path snippets = tempDir.resolve("snippets-slj");
        Path opDir = snippets.resolve("slj-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/slj", "{\"key\":\"value\"}"));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("key", "String", "키"));
        Path output = tempDir.resolve("output-slj");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode example = root.get("paths").get("/api/slj").get("post")
                .get("requestBody").get("content").get("application/json").get("example");
        assertNotNull(example);
        assertEquals("value", example.get("key").asText());
    }

    @Test
    @DisplayName("extractExample: body 없는 요청은 example 없음")
    void extractExample_noBody_noExample() throws Exception {
        Path snippets = tempDir.resolve("snippets-noex");
        Path opDir = snippets.resolve("noex-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("POST", "/api/noex", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("field", "String", "필드"));
        Path output = tempDir.resolve("output-noex");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode mediaType = root.get("paths").get("/api/noex").get("post")
                .get("requestBody").get("content").get("application/json");
        assertNull(mediaType.get("example"));
    }

    // ═══════════════════════════════════════════════════════════════
    // createResponses — 응답 필드
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createResponses: response-fields.adoc 파싱")
    void createResponses_parsesFields() throws Exception {
        Path snippets = tempDir.resolve("snippets-rf");
        Path opDir = snippets.resolve("rf-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/rf", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{\"success\":true}"));
        writeFile(opDir, "response-fields.adoc", """
                |===
                |Path|Type|Description

                |`+success+`
                |`+Boolean+`
                |성공 여부

                |`+data.name+`
                |`+String+`
                |이름

                |===
                """);
        Path output = tempDir.resolve("output-rf");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode schema = root.get("components").get("schemas").get("rf-opResponse");
        assertNotNull(schema);
        assertEquals("boolean", schema.get("properties").get("success").get("type").asText());
        assertEquals("string", schema.get("properties").get("data.name").get("type").asText());
    }

    @Test
    @DisplayName("createResponses: response-fields.adoc 없으면 body 없는 응답")
    void createResponses_noFields_noContent() throws Exception {
        Path snippets = tempDir.resolve("snippets-nrf");
        Path opDir = snippets.resolve("nrf-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("DELETE", "/api/nrf", null));
        writeFile(opDir, "http-response.adoc", httpResponse("204 No Content", ""));
        Path output = tempDir.resolve("output-nrf");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode resp = root.get("paths").get("/api/nrf").get("delete").get("responses").get("204");
        assertNotNull(resp);
        assertNull(resp.get("content"));
    }

    // ═══════════════════════════════════════════════════════════════
    // readMetadata — 메타데이터 읽기
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("readMetadata: tag, summary, description 전부 읽기")
    void readMetadata_allFields() throws Exception {
        Path snippets = tempDir.resolve("snippets-meta");
        Path opDir = snippets.resolve("meta-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/meta", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "metadata.yml", """
                tag: UserAPI
                summary: 사용자 조회
                description: |
                  사용자 정보를 조회합니다.
                  ID로 단일 사용자를 조회합니다.
                """);
        Path output = tempDir.resolve("output-meta");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode op = root.get("paths").get("/api/meta").get("get");
        assertEquals("UserAPI", op.get("tags").get(0).asText());
        assertEquals("사용자 조회", op.get("summary").asText());
        assertNotNull(op.get("description"));
    }

    @Test
    @DisplayName("readMetadata: metadata 없으면 기본값 사용")
    void readMetadata_noFile_defaults() throws Exception {
        Path snippets = tempDir.resolve("snippets-nometa");
        Path opDir = snippets.resolve("user-list");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/nometa", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-nometa");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode op = root.get("paths").get("/api/nometa").get("get");
        assertEquals("user", op.get("tags").get(0).asText()); // extractTag
        assertEquals("user list", op.get("summary").asText()); // generateSummary
        assertNull(op.get("description"));
    }

    @Test
    @DisplayName("readMetadata: tag만 있는 경우")
    void readMetadata_onlyTag() throws Exception {
        Path snippets = tempDir.resolve("snippets-tago");
        Path opDir = snippets.resolve("tago-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/tago", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "metadata.yml", "tag: AdminAPI\n");
        Path output = tempDir.resolve("output-tago");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode op = root.get("paths").get("/api/tago").get("get");
        assertEquals("AdminAPI", op.get("tags").get(0).asText());
    }

    @Test
    @DisplayName("readMetadata: 인라인 description (| 없이)")
    void readMetadata_inlineDescription() throws Exception {
        Path snippets = tempDir.resolve("snippets-inld");
        Path opDir = snippets.resolve("inld-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/inld", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "metadata.yml", "description: 간단한 설명\n");
        Path output = tempDir.resolve("output-inld");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals("간단한 설명", root.get("paths").get("/api/inld").get("get").get("description").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // generateSummary & extractTag
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @CsvSource({
            "create-user, create user",
            "get_all_items, get all items",
            "delete-user-profile, delete user profile",
            "simple, simple"
    })
    @DisplayName("generateSummary: kebab/snake → 공백 변환")
    void generateSummary(String input, String expected) throws Exception {
        Path snippets = tempDir.resolve("snippets-gs-" + input);
        Path opDir = snippets.resolve(input);
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/gs-" + input, null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-gs-" + input);
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals(expected, root.get("paths").get("/api/gs-" + input).get("get").get("summary").asText());
    }

    @ParameterizedTest
    @CsvSource({
            "create-user, create",
            "get_items, get",
            "list-all-products, list",
            "single, single"
    })
    @DisplayName("extractTag: 첫 단어를 태그로")
    void extractTag(String input, String expected) throws Exception {
        Path snippets = tempDir.resolve("snippets-et-" + input);
        Path opDir = snippets.resolve(input);
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/et-" + input, null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        Path output = tempDir.resolve("output-et-" + input);
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals(expected, root.get("paths").get("/api/et-" + input).get("get").get("tags").get(0).asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // 복합 시나리오
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("복합: 같은 path에 여러 method가 합쳐진다")
    void multipleMethodsSamePath() throws Exception {
        Path snippets = tempDir.resolve("snippets-multi");
        Files.createDirectories(snippets);
        createMinimalOperation(snippets, "get-items", "GET", "/api/items");
        createMinimalOperation(snippets, "create-item", "POST", "/api/items");
        Path output = tempDir.resolve("output-multi");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode pathNode = root.get("paths").get("/api/items");
        assertNotNull(pathNode.get("get"));
        assertNotNull(pathNode.get("post"));
    }

    @Test
    @DisplayName("복합: operationId는 디렉토리 이름")
    void operationId_isDirectoryName() throws Exception {
        Path snippets = tempDir.resolve("snippets-opid");
        Files.createDirectories(snippets);
        createMinimalOperation(snippets, "my-custom-operation-id", "GET", "/api/opid");
        Path output = tempDir.resolve("output-opid");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals("my-custom-operation-id",
                root.get("paths").get("/api/opid").get("get").get("operationId").asText());
    }

    @Test
    @DisplayName("복합: path param + query param + header 동시 존재")
    void combinedParams() throws Exception {
        Path snippets = tempDir.resolve("snippets-comb");
        Path opDir = snippets.resolve("comb-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/users/1/orders", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("userId", "사용자 ID"));
        writeFile(opDir, "query-parameters.adoc", queryParamsAdoc("status", "주문 상태"));
        writeFile(opDir, "request-headers.adoc", """
                |===
                |Name|Description

                |`+Authorization+`
                |인증 토큰

                |===
                """);
        Path output = tempDir.resolve("output-comb");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode params = root.get("paths").get("/api/users/{userId}/orders").get("get").get("parameters");
        assertEquals(3, params.size());
        // 순서: query, path, header
        assertEquals("query", params.get(0).get("in").asText());
        assertEquals("path", params.get(1).get("in").asText());
        assertEquals("header", params.get(2).get("in").asText());
    }

    @Test
    @DisplayName("복합: 커스텀 output 파일명")
    void customOutputFileName() throws Exception {
        Path snippets = tempDir.resolve("snippets-custom");
        Files.createDirectories(snippets);
        Path output = tempDir.resolve("output-custom");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));
        properties.getOpenapi().setOutputFileName("custom-api.json");

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        assertTrue(new File(output.toFile(), "custom-api.json").exists());
    }

    // ═══════════════════════════════════════════════════════════════
    // 추가 엣지 케이스
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("서버 URL에 contextPath 반영")
    void serverUrl_includesContextPath() throws Exception {
        Path snippets = tempDir.resolve("snippets-ctx");
        Files.createDirectories(snippets);
        Path output = tempDir.resolve("output-ctx");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));
        properties.getOpenapi().getServer().setContextPath("/api/v2");

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertEquals("http://localhost:8080/api/v2", root.get("servers").get(0).get("url").asText());
    }

    @Test
    @DisplayName("response-fields + response-headers 동시 존재")
    void responseFieldsAndHeaders_together() throws Exception {
        Path snippets = tempDir.resolve("snippets-rfh");
        Path opDir = snippets.resolve("rfh-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/rfh", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{\"ok\":true}"));
        writeFile(opDir, "response-fields.adoc", """
                |===
                |Path|Type|Description

                |`+ok+`
                |`+Boolean+`
                |성공

                |===
                """);
        writeFile(opDir, "response-headers.adoc", """
                |===
                |Name|Description

                |`+X-Trace+`
                |추적 ID

                |===
                """);
        Path output = tempDir.resolve("output-rfh");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode resp200 = root.get("paths").get("/api/rfh").get("get").get("responses").get("200");
        assertNotNull(resp200.get("content")); // body
        assertNotNull(resp200.get("headers")); // headers
        assertEquals("추적 ID", resp200.get("headers").get("X-Trace").get("description").asText());
    }

    @Test
    @DisplayName("MongoDB ObjectId (24자 hex) path param 치환")
    void templatizePath_mongoObjectId() throws Exception {
        Path snippets = tempDir.resolve("snippets-mongo");
        Path opDir = snippets.resolve("mongo-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/docs/507f1f77bcf86cd799439011", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "path-parameters.adoc", pathParamsAdoc("docId", "문서 ID"));
        Path output = tempDir.resolve("output-mongo");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        assertNotNull(root.get("paths").get("/api/docs/{docId}"));
    }

    @Test
    @DisplayName("requestBody example: 중첩 JSON 객체 파싱")
    void extractExample_nestedJson() throws Exception {
        Path snippets = tempDir.resolve("snippets-nest");
        Path opDir = snippets.resolve("nest-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", """
                [source,http,options="nowrap"]
                ----
                POST /api/nested HTTP/1.1
                Content-Type: application/json
                Host: localhost

                {
                  "user" : {
                    "name" : "John",
                    "address" : {
                      "city" : "Seoul"
                    }
                  }
                }
                ----
                """);
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "request-fields.adoc", requestFieldsAdoc("user", "Object", "사용자"));
        Path output = tempDir.resolve("output-nest");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        JsonNode example = root.get("paths").get("/api/nested").get("post")
                .get("requestBody").get("content").get("application/json").get("example");
        assertNotNull(example);
        assertEquals("Seoul", example.get("user").get("address").get("city").asText());
    }

    @Test
    @DisplayName("metadata description: 멀티라인 (빈 줄 포함)")
    void readMetadata_multilineDescription_withBlankLine() throws Exception {
        Path snippets = tempDir.resolve("snippets-mld");
        Path opDir = snippets.resolve("mld-op");
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest("GET", "/api/mld", null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
        writeFile(opDir, "metadata.yml", """
                tag: Test
                summary: 멀티라인 테스트
                description: |
                  첫 번째 줄

                  세 번째 줄
                """);
        Path output = tempDir.resolve("output-mld");
        properties.getOpenapi().setSnippetPaths(List.of(snippets.toAbsolutePath().toString()));

        generator.generateOpenApiJson(null, output.toAbsolutePath().toString());

        JsonNode root = objectMapper.readTree(new File(output.toFile(), "openapi3.json"));
        String desc = root.get("paths").get("/api/mld").get("get").get("description").asText();
        assertTrue(desc.contains("첫 번째 줄"));
        assertTrue(desc.contains("세 번째 줄"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 헬퍼 메서드
    // ═══════════════════════════════════════════════════════════════

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Files.writeString(dir.resolve(filename), content);
    }

    private void createMinimalOperation(Path snippetsDir, String opName, String method, String path) throws IOException {
        Path opDir = snippetsDir.resolve(opName);
        Files.createDirectories(opDir);
        writeFile(opDir, "http-request.adoc", httpRequest(method, path, null));
        writeFile(opDir, "http-response.adoc", httpResponse("200 OK", "{}"));
    }

    private String httpRequest(String method, String path, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("[source,http,options=\"nowrap\"]\n----\n");
        sb.append(method).append(" ").append(path).append(" HTTP/1.1\n");
        sb.append("Host: localhost\n");
        if (body != null) {
            sb.append("Content-Type: application/json\n\n");
            sb.append(body).append("\n");
        } else {
            sb.append("\n");
        }
        sb.append("----\n");
        return sb.toString();
    }

    private String httpResponse(String statusLine, String body) {
        return "[source,http,options=\"nowrap\"]\n----\nHTTP/1.1 " + statusLine + "\nContent-Type: application/json\n\n" + body + "\n----\n";
    }

    private String requestFieldsAdoc(String name, String type, String desc) {
        return "|===\n|Path|Type|Description\n\n|`+" + name + "+`\n|`+" + type + "+`\n|" + desc + "\n\n|===\n";
    }

    private String pathParamsAdoc(String name, String desc) {
        return "|===\n|Name|Description\n\n|`+" + name + "+`\n|" + desc + "\n\n|===\n";
    }

    private String queryParamsAdoc(String name, String desc) {
        return "|===\n|Parameter|Description\n\n|`+" + name + "+`\n|" + desc + "\n\n|===\n";
    }
}
