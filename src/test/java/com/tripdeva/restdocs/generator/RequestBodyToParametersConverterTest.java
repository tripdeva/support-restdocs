package com.tripdeva.restdocs.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RequestBodyToParametersConverterTest {

    private RequestBodyToParametersConverter converter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        converter = new RequestBodyToParametersConverter();
    }

    // ═══════════════════════════════════════════════════════════════
    // POST/PUT/PATCH 건너뛰기
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @ValueSource(strings = {"post", "put", "patch"})
    @DisplayName("POST/PUT/PATCH는 requestBody를 parameters로 변환하지 않는다")
    void skipsPostPutPatch(String method) throws Exception {
        ObjectNode root = createOpenApiWithRequestBody(method, "/api/test", "TestSchema");
        File jsonFile = writeJson(root, "skip-" + method + ".json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode op = result.get("paths").get("/api/test").get(method);
        assertNull(op.get("parameters"), "parameters should not be added for " + method);
    }

    @Test
    @DisplayName("GET의 requestBody는 parameters로 변환한다")
    void convertsGetRequestBody() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/search", "SearchSchema");
        addSchemaProperties(root, "SearchSchema", new String[]{"keyword", "page"}, new String[]{"string", "integer"});
        File jsonFile = writeJson(root, "convert-get.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/search").get("get").get("parameters");
        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals("keyword", params.get(0).get("name").asText());
        assertEquals("query", params.get(0).get("in").asText());
    }

    @Test
    @DisplayName("DELETE의 requestBody는 parameters로 변환한다")
    void convertsDeleteRequestBody() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("delete", "/api/items", "DeleteSchema");
        addSchemaProperties(root, "DeleteSchema", new String[]{"ids"}, new String[]{"array"});
        File jsonFile = writeJson(root, "convert-delete.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/items").get("delete").get("parameters");
        assertNotNull(params);
        assertEquals(1, params.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 파일 관련
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("존재하지 않는 JSON 파일은 무시한다")
    void nonExistentFile_skips() {
        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters("/nonexistent/file.json"));
    }

    @Test
    @DisplayName("잘못된 JSON 구조는 조용히 반환한다")
    void invalidJsonStructure_silentReturn() throws Exception {
        File badFile = tempDir.resolve("bad.json").toFile();
        Files.writeString(badFile.toPath(), "[]"); // ObjectNode가 아닌 ArrayNode
        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(badFile.getAbsolutePath()));
    }

    @Test
    @DisplayName("paths 없는 JSON은 정상 처리")
    void noPaths_skips() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");
        File jsonFile = writeJson(root, "no-paths.json");

        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath()));
    }

    // ═══════════════════════════════════════════════════════════════
    // $ref 해결
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("$ref로 참조된 스키마를 올바르게 해결한다")
    void resolvesSchemaReference() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/ref-test", "RefSchema");
        addSchemaProperties(root, "RefSchema",
                new String[]{"name", "email"},
                new String[]{"string", "string"});
        File jsonFile = writeJson(root, "ref-test.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/ref-test").get("get").get("parameters");
        assertEquals(2, params.size());
    }

    @Test
    @DisplayName("존재하지 않는 $ref는 변환 건너뜀")
    void nonExistentRef_skips() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/badref", "NonExistentSchema");
        // 스키마를 추가하지 않음
        File jsonFile = writeJson(root, "badref.json");

        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath()));
    }

    @Test
    @DisplayName("인라인 스키마(비 $ref)도 처리한다")
    void inlineSchema_processed() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode pathItem = objectMapper.createObjectNode();
        ObjectNode op = objectMapper.createObjectNode();

        // 인라인 스키마로 requestBody 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode mediaType = objectMapper.createObjectNode();
        ObjectNode inlineSchema = objectMapper.createObjectNode();
        inlineSchema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode nameProp = objectMapper.createObjectNode();
        nameProp.put("type", "string");
        nameProp.put("description", "이름");
        props.set("name", nameProp);
        inlineSchema.set("properties", props);
        mediaType.set("schema", inlineSchema);
        content.set("application/json", mediaType);
        requestBody.set("content", content);
        op.set("requestBody", requestBody);

        pathItem.set("get", op);
        paths.set("/api/inline", pathItem);
        root.set("paths", paths);
        root.set("components", objectMapper.createObjectNode());
        File jsonFile = writeJson(root, "inline.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/inline").get("get").get("parameters");
        assertNotNull(params);
        assertEquals("name", params.get(0).get("name").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // required/optional 필드
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("required 필드는 required=true로 변환된다")
    void requiredFields_markedAsRequired() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/req-test", "ReqSchema");
        ObjectNode schema = addSchemaProperties(root, "ReqSchema",
                new String[]{"name", "optional_field"},
                new String[]{"string", "string"});
        ArrayNode required = objectMapper.createArrayNode();
        required.add("name");
        schema.set("required", required);
        File jsonFile = writeJson(root, "req-test.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/req-test").get("get").get("parameters");
        assertTrue(params.get(0).get("required").asBoolean()); // name
        assertFalse(params.get(1).get("required").asBoolean()); // optional_field
    }

    @Test
    @DisplayName("required 배열 없으면 모두 required=false")
    void noRequiredArray_allOptional() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/no-req", "NoReqSchema");
        addSchemaProperties(root, "NoReqSchema", new String[]{"a", "b"}, new String[]{"string", "string"});
        File jsonFile = writeJson(root, "no-req.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/no-req").get("get").get("parameters");
        assertFalse(params.get(0).get("required").asBoolean());
        assertFalse(params.get(1).get("required").asBoolean());
    }

    // ═══════════════════════════════════════════════════════════════
    // description, type, format, example
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("description이 있으면 사용하고, 없으면 property name을 사용한다")
    void description_fallbackToPropertyName() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/desc", "DescSchema");
        ObjectNode schema = addSchemaProperties(root, "DescSchema",
                new String[]{"with_desc", "no_desc"},
                new String[]{"string", "string"});
        ObjectNode props = (ObjectNode) schema.get("properties");
        ((ObjectNode) props.get("with_desc")).put("description", "설명 있음");
        // no_desc는 description 없음
        File jsonFile = writeJson(root, "desc.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/desc").get("get").get("parameters");
        assertEquals("설명 있음", params.get(0).get("description").asText());
        assertEquals("no_desc", params.get(1).get("description").asText());
    }

    @Test
    @DisplayName("type이 없으면 기본 string")
    void noType_defaultsToString() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/notype", "NoTypeSchema");
        ObjectNode schema = addSchemaProperties(root, "NoTypeSchema",
                new String[]{"field"}, new String[]{null});
        // type 제거
        ((ObjectNode) schema.get("properties").get("field")).remove("type");
        File jsonFile = writeJson(root, "notype.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode paramSchema = result.get("paths").get("/api/notype").get("get")
                .get("parameters").get(0).get("schema");
        assertEquals("string", paramSchema.get("type").asText());
    }

    @Test
    @DisplayName("format이 있으면 복사된다")
    void formatIsCopied() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/fmt", "FmtSchema");
        ObjectNode schema = addSchemaProperties(root, "FmtSchema",
                new String[]{"date_field"}, new String[]{"string"});
        ((ObjectNode) schema.get("properties").get("date_field")).put("format", "date-time");
        File jsonFile = writeJson(root, "fmt.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode paramSchema = result.get("paths").get("/api/fmt").get("get")
                .get("parameters").get(0).get("schema");
        assertEquals("date-time", paramSchema.get("format").asText());
    }

    @Test
    @DisplayName("example이 있으면 복사된다")
    void exampleIsCopied() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/ex", "ExSchema");
        ObjectNode schema = addSchemaProperties(root, "ExSchema",
                new String[]{"name"}, new String[]{"string"});
        ((ObjectNode) schema.get("properties").get("name")).put("example", "John");
        File jsonFile = writeJson(root, "ex.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode paramSchema = result.get("paths").get("/api/ex").get("get")
                .get("parameters").get(0).get("schema");
        assertEquals("John", paramSchema.get("example").asText());
    }

    // ═══════════════════════════════════════════════════════════════
    // 기존 parameters 합치기
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("기존 parameters에 추가된다")
    void appendsToExistingParameters() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/existing", "ExistSchema");
        addSchemaProperties(root, "ExistSchema", new String[]{"body_param"}, new String[]{"string"});
        // 기존 parameter 추가
        ObjectNode op = (ObjectNode) root.get("paths").get("/api/existing").get("get");
        ArrayNode existingParams = objectMapper.createArrayNode();
        ObjectNode existingParam = objectMapper.createObjectNode();
        existingParam.put("name", "existing_param");
        existingParam.put("in", "query");
        existingParams.add(existingParam);
        op.set("parameters", existingParams);
        File jsonFile = writeJson(root, "existing.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        JsonNode params = result.get("paths").get("/api/existing").get("get").get("parameters");
        assertEquals(2, params.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 엣지 케이스
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("requestBody 없는 operation은 건너뛴다")
    void noRequestBody_skips() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode pathItem = objectMapper.createObjectNode();
        ObjectNode op = objectMapper.createObjectNode();
        op.put("summary", "test");
        pathItem.set("get", op);
        paths.set("/api/no-rb", pathItem);
        root.set("paths", paths);
        File jsonFile = writeJson(root, "no-rb.json");

        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath()));
    }

    @Test
    @DisplayName("content가 없는 requestBody는 건너뛴다")
    void noContent_skips() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode pathItem = objectMapper.createObjectNode();
        ObjectNode op = objectMapper.createObjectNode();
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("required", true);
        op.set("requestBody", requestBody);
        pathItem.set("get", op);
        paths.set("/api/no-content", pathItem);
        root.set("paths", paths);
        File jsonFile = writeJson(root, "no-content.json");

        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath()));
    }

    @Test
    @DisplayName("properties가 없는 스키마는 건너뛴다")
    void noProperties_skips() throws Exception {
        ObjectNode root = createOpenApiWithRequestBody("get", "/api/no-props", "EmptySchema");
        // 빈 스키마 추가 (properties 없음)
        ObjectNode schemas = (ObjectNode) root.get("components").get("schemas");
        ObjectNode emptySchema = objectMapper.createObjectNode();
        emptySchema.put("type", "object");
        schemas.set("EmptySchema", emptySchema);
        File jsonFile = writeJson(root, "no-props.json");

        assertDoesNotThrow(() ->
                converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath()));
    }

    @Test
    @DisplayName("다중 paths/operations 처리")
    void multiplePaths() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");
        ObjectNode paths = objectMapper.createObjectNode();

        // GET /api/a — requestBody 있음
        ObjectNode pathA = objectMapper.createObjectNode();
        ObjectNode opA = createOperationWithInlineBody("fieldA", "string");
        pathA.set("get", opA);
        paths.set("/api/a", pathA);

        // POST /api/b — requestBody 있음 (건너뛰어야 함)
        ObjectNode pathB = objectMapper.createObjectNode();
        ObjectNode opB = createOperationWithInlineBody("fieldB", "string");
        pathB.set("post", opB);
        paths.set("/api/b", pathB);

        root.set("paths", paths);
        root.set("components", objectMapper.createObjectNode());
        File jsonFile = writeJson(root, "multi-paths.json");

        converter.convertRequestBodyToParameters(jsonFile.getAbsolutePath());

        JsonNode result = objectMapper.readTree(jsonFile);
        assertNotNull(result.get("paths").get("/api/a").get("get").get("parameters")); // 변환됨
        assertNull(result.get("paths").get("/api/b").get("post").get("parameters")); // POST는 건너뜀
    }

    // ═══════════════════════════════════════════════════════════════
    // 헬퍼 메서드
    // ═══════════════════════════════════════════════════════════════

    private File writeJson(ObjectNode root, String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        return file;
    }

    private ObjectNode createOpenApiWithRequestBody(String method, String path, String schemaName) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");

        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode pathItem = objectMapper.createObjectNode();
        ObjectNode op = objectMapper.createObjectNode();

        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode mediaType = objectMapper.createObjectNode();
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$ref", "#/components/schemas/" + schemaName);
        mediaType.set("schema", schema);
        content.set("application/json", mediaType);
        requestBody.set("content", content);
        op.set("requestBody", requestBody);

        pathItem.set(method, op);
        paths.set(path, pathItem);
        root.set("paths", paths);

        ObjectNode components = objectMapper.createObjectNode();
        components.set("schemas", objectMapper.createObjectNode());
        root.set("components", components);

        return root;
    }

    private ObjectNode addSchemaProperties(ObjectNode root, String schemaName, String[] names, String[] types) {
        ObjectNode schemas = (ObjectNode) root.get("components").get("schemas");
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        for (int i = 0; i < names.length; i++) {
            ObjectNode prop = objectMapper.createObjectNode();
            if (types[i] != null) prop.put("type", types[i]);
            props.set(names[i], prop);
        }
        schema.set("properties", props);
        schemas.set(schemaName, schema);
        return schema;
    }

    private ObjectNode createOperationWithInlineBody(String fieldName, String fieldType) {
        ObjectNode op = objectMapper.createObjectNode();
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = objectMapper.createObjectNode();
        ObjectNode mediaType = objectMapper.createObjectNode();
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", fieldType);
        props.set(fieldName, prop);
        schema.set("properties", props);
        mediaType.set("schema", schema);
        content.set("application/json", mediaType);
        requestBody.set("content", content);
        op.set("requestBody", requestBody);
        return op;
    }
}
