package com.tripdeva.restdocs.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripdeva.restdocs.config.OpenApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * REST Docs 스니펫을 기반으로 순수하게 OpenAPI 3.0 JSON을 생성하는 클래스
 */
@Slf4j
@Component
public class OpenApi3Generator {

    private final OpenApiProperties openApiProperties;
    private final ObjectMapper objectMapper;
    private final Pattern operationPattern;
    private final Pattern requestFieldPattern;
    private final Pattern responseFieldPattern;
    private final Pattern pathParameterPattern;
    private final Pattern queryParameterPattern;

    public OpenApi3Generator(OpenApiProperties openApiProperties) {
        this.openApiProperties = openApiProperties;
        this.objectMapper = new ObjectMapper();
        this.operationPattern = Pattern.compile("\\[(.*?)\\]\\s*(\\w+)\\s*(.*?)");
        this.requestFieldPattern = Pattern.compile("\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|");
        this.responseFieldPattern = Pattern.compile("\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|");
        this.pathParameterPattern = Pattern.compile("\\|(.*?)\\|(.*?)\\|(.*?)\\|");
        this.queryParameterPattern = Pattern.compile("\\|(.*?)\\|(.*?)\\|(.*?)\\|");
    }

    /**
     * REST Docs 스니펫을 기반으로 OpenAPI 3.0 JSON 생성
     */
    public void generateOpenApiJson(String snippetsDir, String outputDir) {
        try {
            log.info("Starting OpenAPI 3.0 JSON generation from REST Docs snippets");
            
            // 기본 OpenAPI 구조 생성
            ObjectNode openApiRoot = createBaseOpenApiStructure();
            
            ObjectNode pathsNode = objectMapper.createObjectNode();
            ObjectNode componentsNode = objectMapper.createObjectNode();
            ObjectNode schemasNode = objectMapper.createObjectNode();

            // OpenApiProperties에서 설정된 모든 snippet 경로 처리
            for (String snippetPath : openApiProperties.getSnippetPaths()) {
                log.info("Processing snippet path: {}", snippetPath);
                
                // 상대 경로를 프로젝트 루트 기준으로 해결
                File snippetsDirFile;
                if (new File(snippetPath).isAbsolute()) {
                    snippetsDirFile = new File(snippetPath);
                } else {
                    // 프로젝트 루트 디렉토리를 기준으로 상대 경로 해결
                    String projectRoot = System.getProperty("user.dir");
                    snippetsDirFile = new File(projectRoot, snippetPath);
                }
                
                if (!snippetsDirFile.exists()) {
                    log.warn("Snippets directory not found: {}", snippetsDirFile.getAbsolutePath());
                    continue;
                }

                // 각 operation 디렉토리 처리
                File[] operationDirs = snippetsDirFile.listFiles(File::isDirectory);
                if (operationDirs != null) {
                    for (File operationDir : operationDirs) {
                        processOperationDirectory(operationDir, pathsNode, schemasNode);
                    }
                }
            }

            // paths와 components 추가
            openApiRoot.set("paths", pathsNode);
            componentsNode.set("schemas", schemasNode);
            openApiRoot.set("components", componentsNode);

            // 출력 디렉토리 생성
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }

            // JSON 파일 저장
            File outputFile = new File(outputDir, openApiProperties.getOutputFileName());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, openApiRoot);
            
            log.info("OpenAPI 3.0 JSON generated successfully: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to generate OpenAPI 3.0 JSON", e);
            throw new RuntimeException("OpenAPI 생성 실패", e);
        }
    }

    /**
     * 기본 OpenAPI 3.0 구조 생성 (Properties 기반)
     */
    private ObjectNode createBaseOpenApiStructure() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.0");

        // info 섹션
        ObjectNode info = objectMapper.createObjectNode();
        info.put("title", openApiProperties.getTitle());
        info.put("description", openApiProperties.getDescription());
        info.put("version", openApiProperties.getVersion());
        root.set("info", info);

        // servers 섹션
        ArrayNode servers = objectMapper.createArrayNode();
        ObjectNode server = objectMapper.createObjectNode();
        server.put("url", openApiProperties.getServer());
        servers.add(server);
        root.set("servers", servers);

        return root;
    }

    /**
     * 각 operation 디렉토리 처리
     */
    private void processOperationDirectory(File operationDir, ObjectNode pathsNode, ObjectNode schemasNode) {
        try {
            String operationName = operationDir.getName();
            log.debug("Processing operation: {}", operationName);

            // HTTP 요청 정보 파싱
            File httpRequestFile = new File(operationDir, "http-request.adoc");
            if (!httpRequestFile.exists()) {
                log.warn("http-request.adoc not found in {}", operationDir.getAbsolutePath());
                return;
            }

            HttpRequestInfo requestInfo = parseHttpRequest(httpRequestFile);
            if (requestInfo == null) {
                return;
            }

            // path-parameters.adoc가 있으면 path를 템플릿화
            String resolvedPath = templatizePath(operationDir, requestInfo.path);

            // 해당 path가 이미 존재하는지 확인
            ObjectNode pathItemNode = (ObjectNode) pathsNode.get(resolvedPath);
            if (pathItemNode == null) {
                pathItemNode = objectMapper.createObjectNode();
                pathsNode.set(resolvedPath, pathItemNode);
            }

            // operation 정보 생성
            ObjectNode operationNode = createOperationNode(operationDir, requestInfo, schemasNode);
            pathItemNode.set(requestInfo.method.toLowerCase(), operationNode);

        } catch (Exception e) {
            log.error("Failed to process operation directory: {}", operationDir.getName(), e);
        }
    }

    /**
     * path-parameters.adoc의 파라미터 이름으로 경로를 템플릿화
     * 예: /api/users/1 + path param "id" → /api/users/{id}
     */
    private String templatizePath(File operationDir, String path) {
        File pathParamsFile = new File(operationDir, "path-parameters.adoc");
        if (!pathParamsFile.exists()) {
            return path;
        }

        try {
            List<String> lines = Files.readAllLines(pathParamsFile.toPath());
            List<String> paramNames = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("|`+") && line.endsWith("+`")) {
                    paramNames.add(line.substring(3, line.length() - 2).trim());
                }
            }

            if (paramNames.isEmpty()) {
                return path;
            }

            // 경로의 마지막 세그먼트부터 역순으로 치환
            String[] segments = path.split("/");
            int paramIdx = paramNames.size() - 1;

            for (int i = segments.length - 1; i >= 0 && paramIdx >= 0; i--) {
                String segment = segments[i];
                if (segment.isEmpty()) continue;
                // 숫자 또는 UUID 등 동적 값으로 보이면 치환
                if (segment.matches("\\d+") || segment.matches("[a-f0-9-]{36}") || segment.matches("[a-f0-9]{24}")) {
                    segments[i] = "{" + paramNames.get(paramIdx) + "}";
                    paramIdx--;
                }
            }

            return String.join("/", segments);
        } catch (IOException e) {
            log.warn("Failed to read path-parameters.adoc for path templating in {}", operationDir.getName());
            return path;
        }
    }

    /**
     * HTTP 요청 정보 파싱
     */
    private HttpRequestInfo parseHttpRequest(File httpRequestFile) throws IOException {
        List<String> lines = Files.readAllLines(httpRequestFile.toPath());

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[source,http,options")) {
                continue;
            }
            if (line.startsWith("----")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }

            // HTTP 요청 라인 파싱 (예: "POST /pickup/outbound/scheduled HTTP/1.1")
            String[] parts = line.split(" ");
            if (parts.length >= 2) {
                String method = parts[0].toUpperCase();
                String path = parts[1];
                // query string 제거 — OpenAPI에서는 path와 query를 분리
                int queryIdx = path.indexOf('?');
                if (queryIdx >= 0) {
                    path = path.substring(0, queryIdx);
                }
                return new HttpRequestInfo(method, path);
            }
        }

        return null;
    }

    /**
     * operation 노드 생성
     */
    private ObjectNode createOperationNode(File operationDir, HttpRequestInfo requestInfo, ObjectNode schemasNode) {
        ObjectNode operationNode = objectMapper.createObjectNode();

        // metadata.yml 파일에서 tag, summary, description 읽기
        MetadataInfo metadata = readMetadata(operationDir);
        
        // 기본 정보 (metadata가 있으면 우선 사용, 없으면 기본값)
        operationNode.put("summary", metadata.summary != null ? metadata.summary : generateSummary(operationDir.getName()));
        operationNode.put("operationId", operationDir.getName());
        
        // description 추가 (metadata에 있는 경우)
        if (metadata.description != null) {
            operationNode.put("description", metadata.description);
        }

        // tags 추가 (metadata가 있으면 우선 사용, 없으면 기본값)
        ArrayNode tags = objectMapper.createArrayNode();
        tags.add(metadata.tag != null ? metadata.tag : extractTag(operationDir.getName()));
        operationNode.set("tags", tags);

        // parameters 처리 (query, path, header)
        ArrayNode parameters = objectMapper.createArrayNode();
        addQueryParameters(operationDir, parameters);
        addPathParameters(operationDir, parameters);
        addRequestHeaders(operationDir, parameters);
        if (!parameters.isEmpty()) {
            operationNode.set("parameters", parameters);
        }

        // requestBody 처리 (POST, PUT 등)
        if ("POST".equals(requestInfo.method) || "PUT".equals(requestInfo.method) || "PATCH".equals(requestInfo.method)) {
            ObjectNode requestBody = createRequestBody(operationDir, schemasNode);
            if (requestBody != null) {
                operationNode.set("requestBody", requestBody);
            }
        }

        // responses 처리
        ObjectNode responses = createResponses(operationDir, schemasNode);
        operationNode.set("responses", responses);

        return operationNode;
    }

    /**
     * 요청 파라미터 추가 (query parameters)
     * REST Docs 3.x: query-parameters.adoc (2컬럼: Parameter|Description, 3줄씩)
     * REST Docs 2.x: request-parameters.adoc
     */
    private void addQueryParameters(File operationDir, ArrayNode parameters) {
        File requestParamsFile = new File(operationDir, "query-parameters.adoc");
        if (!requestParamsFile.exists()) {
            requestParamsFile = new File(operationDir, "request-parameters.adoc");
        }
        if (requestParamsFile.exists()) {
            try {
                parseParameterAdoc(requestParamsFile, "query", parameters);
            } catch (IOException e) {
                log.warn("Failed to read query/request-parameters.adoc in {}", operationDir.getName());
            }
        }
    }

    /**
     * 경로 파라미터 추가
     */
    private void addPathParameters(File operationDir, ArrayNode parameters) {
        File pathParamsFile = new File(operationDir, "path-parameters.adoc");
        if (pathParamsFile.exists()) {
            try {
                parseParameterAdoc(pathParamsFile, "path", parameters);
            } catch (IOException e) {
                log.warn("Failed to read path-parameters.adoc in {}", operationDir.getName());
            }
        }
    }

    /**
     * 요청 헤더 추가 (OpenAPI parameters with "in": "header")
     */
    private void addRequestHeaders(File operationDir, ArrayNode parameters) {
        File headersFile = new File(operationDir, "request-headers.adoc");
        if (headersFile.exists()) {
            try {
                parseParameterAdoc(headersFile, "header", parameters);
            } catch (IOException e) {
                log.warn("Failed to read request-headers.adoc in {}", operationDir.getName());
            }
        }
    }

    /**
     * REST Docs 파라미터 adoc 파일 파싱 (query-parameters, path-parameters, request-parameters 공통)
     * REST Docs 3.x 형식: 2줄씩 (이름 행, 설명 행)
     *   |`+name+`
     *   |설명
     */
    private void parseParameterAdoc(File adocFile, String inLocation, ArrayNode parameters) throws IOException {
        List<String> lines = Files.readAllLines(adocFile.toPath());
        String currentName = null;

        for (String line : lines) {
            line = line.trim();

            // 헤더, 구분선 무시
            if (line.equals("|===") || line.isEmpty() || line.startsWith("|Parameter") || line.startsWith("|Name")) {
                continue;
            }

            // 이름 행: |`+paramName+`
            if (line.startsWith("|`+") && line.endsWith("+`")) {
                currentName = line.substring(3, line.length() - 2).trim();
                continue;
            }

            // 설명 행: |설명 텍스트
            if (currentName != null && line.startsWith("|")) {
                String description = line.substring(1).trim();

                ObjectNode param = objectMapper.createObjectNode();
                param.put("name", currentName);
                param.put("in", inLocation);
                param.put("description", description);
                param.put("required", "path".equals(inLocation));

                ObjectNode schema = objectMapper.createObjectNode();
                schema.put("type", "string");
                param.set("schema", schema);

                parameters.add(param);
                currentName = null;
            }
        }
    }

    /**
     * requestBody 생성
     */
    private ObjectNode createRequestBody(File operationDir, ObjectNode schemasNode) {
        File requestFieldsFile = new File(operationDir, "request-fields.adoc");
        if (!requestFieldsFile.exists()) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(requestFieldsFile.toPath());
            ObjectNode properties = objectMapper.createObjectNode();
            List<String> required = new ArrayList<>();

            // REST Docs 형식: 3줄씩 묶여있음 (필드명, 타입, 설명)
            String currentFieldName = null;
            String currentFieldType = null;
            String currentDescription = null;
            int lineIndex = 0;
            
            for (String line : lines) {
                line = line.trim();
                
                // 헤더나 구분선 무시
                if (line.equals("|===") || line.equals("|Path|Type|Description") || line.isEmpty()) {
                    continue;
                }
                
                if (lineIndex == 0 && line.startsWith("|`+") && line.endsWith("+`")) {
                    // 필드명 라인 (예: |`+hawb_no+`)
                    currentFieldName = line.substring(3, line.length() - 2).trim();
                    lineIndex = 1;
                } else if (lineIndex == 1 && line.startsWith("|`+") && line.endsWith("+`")) {
                    // 타입 라인 (예: |`+String+` 또는 |`+String (optional)+`)
                    currentFieldType = line.substring(3, line.length() - 2).trim();
                    lineIndex = 2;
                } else if (lineIndex == 2 && line.startsWith("|")) {
                    // 설명 라인 (예: |운송장 번호)
                    currentDescription = line.substring(1).trim();
                    
                    // 3개 정보가 모두 모이면 property 생성
                    if (currentFieldName != null && currentFieldType != null && currentDescription != null) {
                        ObjectNode property = objectMapper.createObjectNode();
                        
                        // 타입에서 "(optional)" 제거 후 파싱
                        String cleanType = currentFieldType.replace(" (optional)", "").trim();
                        property.put("type", parseFieldType(cleanType));
                        property.put("description", currentDescription);
                        
                        properties.set(currentFieldName, property);
                        
                        // 타입에 "(optional)"이 포함되어 있지 않으면 required 필드로 처리
                        if (!currentFieldType.toLowerCase().contains("(optional)")) {
                            required.add(currentFieldName);
                        }
                        
                        // 초기화
                        currentFieldName = null;
                        currentFieldType = null;
                        currentDescription = null;
                        lineIndex = 0;
                    }
                }
            }

            if (!properties.isEmpty()) {
                // 스키마 생성
                String schemaName = operationDir.getName() + "Request";
                ObjectNode schema = objectMapper.createObjectNode();
                schema.put("type", "object");
                schema.set("properties", properties);
                if (!required.isEmpty()) {
                    ArrayNode requiredArray = objectMapper.createArrayNode();
                    required.forEach(requiredArray::add);
                    schema.set("required", requiredArray);
                }
                schemasNode.set(schemaName, schema);

                // HTTP 요청에서 example 추출
                Object example = extractExampleFromHttpRequest(operationDir);

                // requestBody 생성
                ObjectNode requestBody = objectMapper.createObjectNode();
                ObjectNode content = objectMapper.createObjectNode();
                ObjectNode mediaType = objectMapper.createObjectNode();
                ObjectNode schemaRef = objectMapper.createObjectNode();
                schemaRef.put("$ref", "#/components/schemas/" + schemaName);
                mediaType.set("schema", schemaRef);
                
                // example이 있으면 추가
                if (example != null) {
                    mediaType.set("example", objectMapper.valueToTree(example));
                }
                
                content.set("application/json", mediaType);
                requestBody.set("content", content);
                requestBody.put("required", true);

                return requestBody;
            }

        } catch (IOException e) {
            log.warn("Failed to read request-fields.adoc in {}", operationDir.getName());
        }

        return null;
    }

    /**
     * responses 생성
     */
    private ObjectNode createResponses(File operationDir, ObjectNode schemasNode) {
        ObjectNode responses = objectMapper.createObjectNode();

        // http-response.adoc에서 실제 상태코드 추출
        String statusCode = extractResponseStatusCode(operationDir);

        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("description", "성공");

        // response-fields.adoc가 있으면 처리
        File responseFieldsFile = new File(operationDir, "response-fields.adoc");
        if (responseFieldsFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(responseFieldsFile.toPath());
                ObjectNode properties = objectMapper.createObjectNode();

                // REST Docs 형식: 3줄씩 묶여있음 (필드명, 타입, 설명)
                String currentFieldName = null;
                String currentFieldType = null;
                String currentDescription = null;
                int lineIndex = 0;

                for (String line : lines) {
                    line = line.trim();

                    // 헤더나 구분선 무시
                    if (line.equals("|===") || line.equals("|Path|Type|Description") || line.isEmpty()) {
                        continue;
                    }

                    if (lineIndex == 0 && line.startsWith("|`+") && line.endsWith("+`")) {
                        currentFieldName = line.substring(3, line.length() - 2).trim();
                        lineIndex = 1;
                    } else if (lineIndex == 1 && line.startsWith("|`+") && line.endsWith("+`")) {
                        currentFieldType = line.substring(3, line.length() - 2).trim();
                        lineIndex = 2;
                    } else if (lineIndex == 2 && line.startsWith("|")) {
                        currentDescription = line.substring(1).trim();

                        if (currentFieldName != null && currentFieldType != null && currentDescription != null) {
                            ObjectNode property = objectMapper.createObjectNode();
                            property.put("type", parseFieldType(currentFieldType));
                            property.put("description", currentDescription);

                            properties.set(currentFieldName, property);

                            currentFieldName = null;
                            currentFieldType = null;
                            currentDescription = null;
                            lineIndex = 0;
                        }
                    }
                }

                if (!properties.isEmpty()) {
                    String schemaName = operationDir.getName() + "Response";
                    ObjectNode schema = objectMapper.createObjectNode();
                    schema.put("type", "object");
                    schema.set("properties", properties);
                    schemasNode.set(schemaName, schema);

                    ObjectNode content = objectMapper.createObjectNode();
                    ObjectNode mediaType = objectMapper.createObjectNode();
                    ObjectNode schemaRef = objectMapper.createObjectNode();
                    schemaRef.put("$ref", "#/components/schemas/" + schemaName);
                    mediaType.set("schema", schemaRef);
                    content.set("application/json", mediaType);
                    responseNode.set("content", content);
                }

            } catch (IOException e) {
                log.warn("Failed to read response-fields.adoc in {}", operationDir.getName());
            }
        }

        // response-headers.adoc가 있으면 처리
        ObjectNode responseHeaders = parseResponseHeaders(operationDir);
        if (responseHeaders != null) {
            responseNode.set("headers", responseHeaders);
        }

        responses.set(statusCode, responseNode);
        return responses;
    }

    /**
     * response-headers.adoc 파싱
     */
    private ObjectNode parseResponseHeaders(File operationDir) {
        File headersFile = new File(operationDir, "response-headers.adoc");
        if (!headersFile.exists()) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(headersFile.toPath());
            ObjectNode headers = objectMapper.createObjectNode();
            String currentName = null;

            for (String line : lines) {
                line = line.trim();
                if (line.equals("|===") || line.isEmpty() || line.startsWith("|Name")) {
                    continue;
                }
                if (line.startsWith("|`+") && line.endsWith("+`")) {
                    currentName = line.substring(3, line.length() - 2).trim();
                    continue;
                }
                if (currentName != null && line.startsWith("|")) {
                    String description = line.substring(1).trim();
                    ObjectNode headerNode = objectMapper.createObjectNode();
                    headerNode.put("description", description);
                    ObjectNode schema = objectMapper.createObjectNode();
                    schema.put("type", "string");
                    headerNode.set("schema", schema);
                    headers.set(currentName, headerNode);
                    currentName = null;
                }
            }

            return headers.isEmpty() ? null : headers;
        } catch (IOException e) {
            log.warn("Failed to read response-headers.adoc in {}", operationDir.getName());
            return null;
        }
    }

    /**
     * http-response.adoc에서 실제 HTTP 상태코드 추출
     */
    private String extractResponseStatusCode(File operationDir) {
        File httpResponseFile = new File(operationDir, "http-response.adoc");
        if (!httpResponseFile.exists()) {
            return "200";
        }

        try {
            List<String> lines = Files.readAllLines(httpResponseFile.toPath());
            for (String line : lines) {
                line = line.trim();
                // HTTP/1.1 201 Created 형태의 라인 탐색
                if (line.startsWith("HTTP/") && line.contains(" ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String code = parts[1];
                        if (code.matches("\\d{3}")) {
                            return code;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read http-response.adoc in {}", operationDir.getName());
        }

        return "200";
    }

    /**
     * 필드 타입 파싱
     */
    private String parseFieldType(String fieldType) {
        fieldType = fieldType.toLowerCase().trim();
        
        if (fieldType.contains("string")) return "string";
        if (fieldType.contains("integer")) return "integer";
        if (fieldType.contains("number")) return "number";
        if (fieldType.contains("boolean")) return "boolean";
        if (fieldType.contains("array")) return "array";
        if (fieldType.contains("object")) return "object";
        
        return "string"; // 기본값
    }

    /**
     * 요약 생성
     */
    private String generateSummary(String operationName) {
        // kebab-case를 공백으로 변환
        return operationName.replace("-", " ").replace("_", " ");
    }

    /**
     * 태그 추출
     */
    private String extractTag(String operationName) {
        // 첫 번째 단어를 태그로 사용
        String[] parts = operationName.split("[-_]");
        return parts.length > 0 ? parts[0] : "default";
    }

    /**
     * metadata.yml 파일에서 메타데이터 읽기
     */
    private MetadataInfo readMetadata(File operationDir) {
        File metadataFile = new File(operationDir, "metadata.yml");
        if (!metadataFile.exists()) {
            return new MetadataInfo(null, null, null);
        }

        try {
            List<String> lines = Files.readAllLines(metadataFile.toPath());
            String tag = null, summary = null, description = null;
            boolean inDescription = false;
            StringBuilder descriptionBuilder = new StringBuilder();

            for (String line : lines) {
                if (line.startsWith("tag:")) {
                    tag = line.substring(4).trim();
                } else if (line.startsWith("summary:")) {
                    summary = line.substring(8).trim();
                } else if (line.startsWith("description:")) {
                    inDescription = true;
                    String desc = line.substring(12).trim();
                    if (!desc.isEmpty() && !desc.equals("|")) {
                        descriptionBuilder.append(desc);
                    }
                } else if (inDescription) {
                    // description의 연속 라인 처리
                    if (line.startsWith("  ")) {
                        // 들여쓰기된 라인은 description의 일부
                        if (!descriptionBuilder.isEmpty()) {
                            descriptionBuilder.append("\n");
                        }
                        descriptionBuilder.append(line.substring(2));
                    } else if (line.trim().isEmpty()) {
                        // 빈 라인도 description의 일부로 처리
                        if (!descriptionBuilder.isEmpty()) {
                            descriptionBuilder.append("\n");
                        }
                    } else {
                        // 들여쓰기가 없는 라인이 나오면 description 끝
                        inDescription = false;
                    }
                }
            }

            if (!descriptionBuilder.isEmpty()) {
                description = descriptionBuilder.toString();
            }

            return new MetadataInfo(tag, summary, description);

        } catch (IOException e) {
            log.warn("Failed to read metadata.yml in {}", operationDir.getName());
            return new MetadataInfo(null, null, null);
        }
    }

    /**
     * HTTP 요청에서 example JSON 추출
     */
    private Object extractExampleFromHttpRequest(File operationDir) {
        File httpRequestFile = new File(operationDir, "http-request.adoc");
        if (!httpRequestFile.exists()) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(httpRequestFile.toPath());
            boolean inBody = false;
            StringBuilder jsonBody = new StringBuilder();

            for (String line : lines) {
                // 빈 라인을 만나면 HTTP body 시작
                if (line.trim().isEmpty() && !inBody) {
                    inBody = true;
                    continue;
                }

                // HTTP body 영역에서 JSON 라인 찾기 (멀티라인 지원)
                if (inBody) {
                    String trimmed = line.trim();
                    if (trimmed.equals("----")) {
                        break;
                    }
                    if (!trimmed.isEmpty()) {
                        jsonBody.append(trimmed);
                    }
                }
            }

            if (!jsonBody.isEmpty()) {
                // JSON 파싱하여 Object로 반환
                return objectMapper.readValue(jsonBody.toString(), Object.class);
            }

        } catch (Exception e) {
            log.warn("Failed to extract example from http-request.adoc in {}: {}", operationDir.getName(), e.getMessage());
        }

        return null;
    }


	/**
	 * 메타데이터 정보 클래스
	 */
	private record MetadataInfo(String tag, String summary, String description) {
	}

	/**
	 * HTTP 요청 정보 클래스
	 */
	private record HttpRequestInfo(String method, String path) {
	}
}