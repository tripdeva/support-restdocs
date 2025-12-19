package com.tripdeva.restdocs.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * OpenAPI JSON에서 Request Body 필드를 Parameters로 변환하는 클래스
 * 이를 통해 Swagger UI에서 Request Body 필드들이 Parameters 섹션에 표시되도록 함
 */
@Slf4j
@Component
public class RequestBodyToParametersConverter {

    private final ObjectMapper objectMapper;

    public RequestBodyToParametersConverter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * OpenAPI JSON 파일에서 Request Body를 Parameters로 변환
     */
    public void convertRequestBodyToParameters(String openApiJsonPath) {
        try {
            log.info("Converting Request Body fields to Parameters in OpenAPI JSON: {}", openApiJsonPath);

            File openApiFile = new File(openApiJsonPath);
            if (!openApiFile.exists()) {
                log.warn("OpenAPI JSON file not found: {}", openApiJsonPath);
                return;
            }

            // JSON 파일 읽기
            JsonNode rootNode = objectMapper.readTree(openApiFile);
            if (!(rootNode instanceof ObjectNode)) {
                log.warn("Invalid OpenAPI JSON structure");
                return;
            }

            ObjectNode root = (ObjectNode) rootNode;
            
            // paths 섹션 처리
            JsonNode pathsNode = root.get("paths");
            if (pathsNode != null && pathsNode.isObject()) {
                processPathsForRequestBodyConversion((ObjectNode) pathsNode, root);
            }

            // 변환된 JSON 파일 저장
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(openApiFile, root);
            
            log.info("Successfully converted Request Body fields to Parameters");

        } catch (Exception e) {
            log.error("Failed to convert Request Body to Parameters", e);
            throw new RuntimeException("Request Body 변환 실패", e);
        }
    }

    /**
     * paths 섹션의 모든 operation을 순회하며 Request Body를 Parameters로 변환
     */
    private void processPathsForRequestBodyConversion(ObjectNode pathsNode, ObjectNode rootNode) {
        Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
        
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            String pathName = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();
            
            if (pathItem.isObject()) {
                processPathItemForRequestBodyConversion(pathName, (ObjectNode) pathItem, rootNode);
            }
        }
    }

    /**
     * 특정 path의 모든 HTTP 메서드를 처리
     */
    private void processPathItemForRequestBodyConversion(String pathName, ObjectNode pathItemNode, ObjectNode rootNode) {
        // HTTP 메서드들 (get, post, put, delete, patch 등)
        String[] httpMethods = {"get", "post", "put", "delete", "patch", "head", "options", "trace"};
        
        for (String method : httpMethods) {
            JsonNode operationNode = pathItemNode.get(method);
            if (operationNode != null && operationNode.isObject()) {
                processOperationForRequestBodyConversion(pathName, method, (ObjectNode) operationNode, rootNode);
            }
        }
    }

    /**
     * 특정 operation의 Request Body를 Parameters로 변환
     */
    private void processOperationForRequestBodyConversion(String pathName, String method, ObjectNode operationNode, ObjectNode rootNode) {
        JsonNode requestBodyNode = operationNode.get("requestBody");
        if (requestBodyNode == null || !requestBodyNode.isObject()) {
            return; // Request Body가 없으면 변환할 필요 없음
        }

        log.debug("Converting Request Body to Parameters for {} {}", method.toUpperCase(), pathName);

        try {
            // Request Body에서 스키마 추출
            JsonNode schemaNode = extractSchemaFromRequestBody(requestBodyNode, rootNode);
            if (schemaNode == null) {
                return;
            }

            // 기존 parameters 배열 가져오기 또는 새로 생성
            ArrayNode parametersArray = getOrCreateParametersArray(operationNode);

            // Request Body 스키마의 properties를 parameters로 변환
            convertSchemaPropertiesToParameters(schemaNode, parametersArray, rootNode);

            // Request Body 제거 (선택적)
            // operationNode.remove("requestBody");

            log.debug("Successfully converted Request Body to {} parameters for {} {}", 
                     parametersArray.size(), method.toUpperCase(), pathName);

        } catch (Exception e) {
            log.warn("Failed to convert Request Body for {} {}: {}", method.toUpperCase(), pathName, e.getMessage());
        }
    }

    /**
     * Request Body에서 스키마 추출
     */
    private JsonNode extractSchemaFromRequestBody(JsonNode requestBodyNode, ObjectNode rootNode) {
        // content -> application/json -> schema 경로로 스키마 찾기
        JsonNode contentNode = requestBodyNode.get("content");
        if (contentNode == null || !contentNode.isObject()) {
            return null;
        }

        JsonNode applicationJsonNode = contentNode.get("application/json");
        if (applicationJsonNode == null || !applicationJsonNode.isObject()) {
            return null;
        }

        JsonNode schemaNode = applicationJsonNode.get("schema");
        if (schemaNode == null) {
            return null;
        }

        // $ref인 경우 실제 스키마 해결
        if (schemaNode.has("$ref")) {
            String ref = schemaNode.get("$ref").asText();
            return resolveSchemaReference(ref, rootNode);
        }

        return schemaNode;
    }

    /**
     * $ref를 통해 실제 스키마 해결
     */
    private JsonNode resolveSchemaReference(String ref, ObjectNode rootNode) {
        // #/components/schemas/SchemaName 형태의 참조 해결
        if (ref.startsWith("#/components/schemas/")) {
            String schemaName = ref.substring("#/components/schemas/".length());
            JsonNode componentsNode = rootNode.get("components");
            if (componentsNode != null && componentsNode.isObject()) {
                JsonNode schemasNode = componentsNode.get("schemas");
                if (schemasNode != null && schemasNode.isObject()) {
                    return schemasNode.get(schemaName);
                }
            }
        }
        return null;
    }

    /**
     * operation의 parameters 배열을 가져오거나 새로 생성
     */
    private ArrayNode getOrCreateParametersArray(ObjectNode operationNode) {
        JsonNode parametersNode = operationNode.get("parameters");
        if (parametersNode != null && parametersNode.isArray()) {
            return (ArrayNode) parametersNode;
        }
        
        ArrayNode newParametersArray = objectMapper.createArrayNode();
        operationNode.set("parameters", newParametersArray);
        return newParametersArray;
    }

    /**
     * 스키마의 properties를 parameters로 변환
     */
    private void convertSchemaPropertiesToParameters(JsonNode schemaNode, ArrayNode parametersArray, ObjectNode rootNode) {
        JsonNode propertiesNode = schemaNode.get("properties");
        if (propertiesNode == null || !propertiesNode.isObject()) {
            return;
        }

        JsonNode requiredNode = schemaNode.get("required");
        ArrayNode requiredArray = (requiredNode != null && requiredNode.isArray()) ? (ArrayNode) requiredNode : null;

        Iterator<Map.Entry<String, JsonNode>> propertyIterator = propertiesNode.fields();
        while (propertyIterator.hasNext()) {
            Map.Entry<String, JsonNode> propertyEntry = propertyIterator.next();
            String propertyName = propertyEntry.getKey();
            JsonNode propertySchema = propertyEntry.getValue();

            // parameter 객체 생성
            ObjectNode parameterNode = createParameterFromProperty(propertyName, propertySchema, requiredArray);
            parametersArray.add(parameterNode);
        }
    }

    /**
     * 속성으로부터 parameter 객체 생성
     */
    private ObjectNode createParameterFromProperty(String propertyName, JsonNode propertySchema, ArrayNode requiredArray) {
        ObjectNode parameterNode = objectMapper.createObjectNode();
        
        // 기본 parameter 정보
        parameterNode.put("name", propertyName);
        parameterNode.put("in", "formData"); // Request Body 필드를 formData parameter로 변환
        
        // 필수 여부 확인
        boolean isRequired = false;
        if (requiredArray != null) {
            for (JsonNode requiredField : requiredArray) {
                if (propertyName.equals(requiredField.asText())) {
                    isRequired = true;
                    break;
                }
            }
        }
        parameterNode.put("required", isRequired);

        // description 추가
        if (propertySchema.has("description")) {
            parameterNode.put("description", propertySchema.get("description").asText());
        } else {
            parameterNode.put("description", propertyName);
        }

        // schema 추가 (타입 정보)
        ObjectNode schemaNode = objectMapper.createObjectNode();
        
        if (propertySchema.has("type")) {
            schemaNode.put("type", propertySchema.get("type").asText());
        } else {
            schemaNode.put("type", "string"); // 기본값
        }

        if (propertySchema.has("format")) {
            schemaNode.put("format", propertySchema.get("format").asText());
        }

        if (propertySchema.has("example")) {
            schemaNode.set("example", propertySchema.get("example"));
        }

        parameterNode.set("schema", schemaNode);

        return parameterNode;
    }
}