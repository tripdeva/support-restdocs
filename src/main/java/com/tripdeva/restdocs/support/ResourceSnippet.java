package com.tripdeva.restdocs.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.restdocs.snippet.Snippet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * epages 스타일 Resource Builder
 * 기존 epages 코드와의 호환성을 위한 클래스
 */
public class ResourceSnippet {

	/**
	 * ResourceBuilder 인스턴스 생성
	 *
	 * @return ResourceBuilder
	 */
	public static ResourceBuilder resource() {
		return new ResourceBuilder();
	}

	/**
	 * Schema 인스턴스 생성
	 *
	 * @param schemaName 스키마 이름
	 * @return Schema
	 */
	public static Schema schema(String schemaName) {
		return new Schema(schemaName);
	}

	/**
	 * epages 스타일 ResourceBuilder 구현
	 */
	@Getter
	public static class ResourceBuilder {
		// Getters for OpenAPI generator
		private String tag;
		private String summary;
		private String description;
		private Schema requestSchema;
		private Schema responseSchema;
		private FieldDescriptor[] requestFields;
		private FieldDescriptor[] responseFields;
		private ParameterDescriptor[] queryParameters;
		private ParameterDescriptor[] pathParameters;

		public ResourceBuilder tag(String tag) {
			this.tag = tag;
			return this;
		}

		public ResourceBuilder summary(String summary) {
			this.summary = summary;
			return this;
		}

		public ResourceBuilder description(String description) {
			this.description = description;
			return this;
		}

		public ResourceBuilder requestSchema(Schema schema) {
			this.requestSchema = schema;
			return this;
		}

		public ResourceBuilder responseSchema(Schema schema) {
			this.responseSchema = schema;
			return this;
		}

		public ResourceBuilder requestFields(FieldDescriptor... fields) {
			this.requestFields = fields;
			return this;
		}

		public ResourceBuilder responseFields(FieldDescriptor... fields) {
			this.responseFields = fields;
			return this;
		}

		public ResourceBuilder queryParameters(ParameterDescriptor... parameters) {
			this.queryParameters = parameters;
			return this;
		}
		
		public ResourceBuilder queryParameters(TypedParameterDescriptor... parameters) {
			this.queryParameters = java.util.Arrays.stream(parameters)
					.map(TypedParameterDescriptor::getDelegate)
					.toArray(ParameterDescriptor[]::new);
			return this;
		}

		public ResourceBuilder pathParameters(ParameterDescriptor... parameters) {
			this.pathParameters = parameters;
			return this;
		}
		
		public ResourceBuilder pathParameters(TypedParameterDescriptor... parameters) {
			this.pathParameters = java.util.Arrays.stream(parameters)
					.map(TypedParameterDescriptor::getDelegate)
					.toArray(ParameterDescriptor[]::new);
			return this;
		}

		/**
		 * ResourceBuilder를 REST Docs snippet들로 변환
		 *
		 * @return REST Docs snippets 배열
		 */
		public org.springframework.restdocs.snippet.Snippet[] build() {
			java.util.List<org.springframework.restdocs.snippet.Snippet> snippets = new java.util.ArrayList<>();

			// 메타데이터를 위한 커스텀 snippet 추가 (tag, summary, description)
			if (tag != null || summary != null || description != null) {
				snippets.add(new MetadataSnippet(tag, summary, description));
			}

			// 요청 필드가 있으면 추가
			if (requestFields != null && requestFields.length > 0) {
				snippets.add(PayloadDocumentation.requestFields(requestFields));
				// optional 정보 저장
				snippets.add(new OptionalFieldsSnippet(requestFields));
			}

			// 응답 필드가 있으면 추가
			if (responseFields != null && responseFields.length > 0) {
				snippets.add(PayloadDocumentation.responseFields(responseFields));
			}

			// 쿼리 파라미터가 있으면 추가
			if (queryParameters != null && queryParameters.length > 0) {
				snippets.add(RequestDocumentation.queryParameters(queryParameters));
			}

			// 경로 파라미터가 있으면 추가
			if (pathParameters != null && pathParameters.length > 0) {
				snippets.add(RequestDocumentation.pathParameters(pathParameters));
			}

			return snippets.toArray(new org.springframework.restdocs.snippet.Snippet[0]);
		}

	}

	/**
	 * epages 스타일 Schema 구현
	 */
	public record Schema(String name) {
	}

	/**
	 * 메타데이터(tag, summary, description)를 기록하는 커스텀 snippet
	 */
	public record MetadataSnippet(String tag, String summary, String description) implements Snippet {

		@Override
		public void document(Operation operation) throws IOException {
			// 메타데이터를 별도 파일로 저장
			Object outputDirObj = operation.getAttributes().get("org.springframework.restdocs.outputDir");
			Path snippetsDir = outputDirObj != null ?
					Paths.get(outputDirObj.toString()) :
					Paths.get("build/generated-snippets");

			Path operationDir = snippetsDir.resolve(operation.getName());
			Files.createDirectories(operationDir);

			// metadata.yml 파일 생성
			Path metadataFile = operationDir.resolve("metadata.yml");
			StringBuilder content = new StringBuilder();

			if (tag != null) {
				content.append("tag: ").append(tag).append("\n");
			}
			if (summary != null) {
				content.append("summary: ").append(summary).append("\n");
			}
			if (description != null) {
				content.append("description: |\n");
				for (String line : description.split("\n")) {
					content.append("  ").append(line).append("\n");
				}
			}

			Files.write(metadataFile, content.toString().getBytes(StandardCharsets.UTF_8));
		}


	}

	/**
	 * optional 필드 정보를 request-fields.adoc에 추가하는 커스텀 snippet
	 */
	@Slf4j
	public static class OptionalFieldsSnippet implements Snippet {
		private final FieldDescriptor[] fields;

		public OptionalFieldsSnippet(FieldDescriptor[] fields) {
			this.fields = fields;
		}

		@Override
		public void document(Operation operation) throws IOException {
			log.debug("OptionalFieldsSnippet.document() called for operation: {}", operation.getName());
			
			Object outputDirObj = operation.getAttributes().get("org.springframework.restdocs.outputDir");
			Path snippetsDir = outputDirObj != null ?
					Paths.get(outputDirObj.toString()) :
					Paths.get("build/generated-snippets");

			Path operationDir = snippetsDir.resolve(operation.getName());
			Path requestFieldsFile = operationDir.resolve("request-fields.adoc");
			
			// request-fields.adoc가 존재하는지 확인하고 optional 정보 추가
			if (Files.exists(requestFieldsFile)) {
				List<String> originalLines = Files.readAllLines(requestFieldsFile, StandardCharsets.UTF_8);
				List<String> modifiedLines = new ArrayList<>();
				
				// optional 필드 목록 생성
				Set<String> optionalFieldNames = new HashSet<>();
				for (FieldDescriptor field : fields) {
					try {
						if (field.isOptional()) {
							optionalFieldNames.add(field.getPath());
							log.debug("Field {} is optional", field.getPath());
						}
					} catch (Exception e) {
						log.debug("Failed to check optional for field {}: {}", field.getPath(), e.getMessage());
					}
				}
				
				log.debug("Optional fields: {}", optionalFieldNames);
				
				// .adoc 파일의 타입 컬럼에 optional 표시 추가
				for (int i = 0; i < originalLines.size(); i++) {
					String line = originalLines.get(i);
					modifiedLines.add(line);
					
					// 필드명 라인인지 확인
					if (line.startsWith("|`+") && line.endsWith("+`")) {
						String fieldName = line.substring(3, line.length() - 2).trim();
						
						// 타입 라인이 다음에 오는지 확인하고 optional 표시 추가
						if (i + 1 < originalLines.size()) {
							String typeLine = originalLines.get(i + 1);
							if (typeLine.startsWith("|`+") && typeLine.endsWith("+`") && optionalFieldNames.contains(fieldName)) {
								// 타입 라인에 " (optional)" 추가
								String typeContent = typeLine.substring(3, typeLine.length() - 2).trim();
								String modifiedTypeLine = "|`+" + typeContent + " (optional)+`";
								modifiedLines.add(modifiedTypeLine);
								i++; // 타입 라인을 이미 처리했으므로 건너뛰기
								log.debug("Modified type line for field {}: {} -> {}", fieldName, typeLine, modifiedTypeLine);
								continue;
							}
						}
					}
				}
				
				// 수정된 내용을 다시 파일에 쓰기
				Files.write(requestFieldsFile, modifiedLines, StandardCharsets.UTF_8);
				log.debug("Updated request-fields.adoc with optional field markers");
			} else {
				log.debug("request-fields.adoc not found at: {}", requestFieldsFile);
			}
		}
	}

	/**
	 * 타입 정보를 포함할 수 있는 확장 ParameterDescriptor
	 */
	public static class TypedParameterDescriptor {
		private final ParameterDescriptor delegate;
		private JsonFieldType type;

		public TypedParameterDescriptor(String name) {
			this.delegate = RequestDocumentation.parameterWithName(name);
		}

		public TypedParameterDescriptor type(JsonFieldType type) {
			this.type = type;
			return this;
		}

		public TypedParameterDescriptor description(String description) {
			this.delegate.description(description);
			return this;
		}

		public TypedParameterDescriptor optional() {
			this.delegate.optional();
			return this;
		}

		public TypedParameterDescriptor defaultValue(String defaultValue) {
			this.delegate.attributes(Attributes.key("defaultValue").value(defaultValue));
			return this;
		}

		public ParameterDescriptor getDelegate() {
			return delegate;
		}

		public JsonFieldType getType() {
			return type;
		}
	}

	/**
	 * 타입을 지원하는 parameterWithName 메서드
	 *
	 * @param name 파라미터 이름
	 * @return TypedParameterDescriptor
	 */
	public static TypedParameterDescriptor parameterWithName(String name) {
		return new TypedParameterDescriptor(name);
	}

	// JsonFieldType constants for convenience
	public static final JsonFieldType BOOLEAN = JsonFieldType.BOOLEAN;
	public static final JsonFieldType STRING = JsonFieldType.STRING;
	public static final JsonFieldType NUMBER = JsonFieldType.NUMBER;
	public static final JsonFieldType ARRAY = JsonFieldType.ARRAY;
	public static final JsonFieldType OBJECT = JsonFieldType.OBJECT;
	public static final JsonFieldType NULL = JsonFieldType.NULL;
	public static final JsonFieldType SIMPLE_BOOLEAN = JsonFieldType.BOOLEAN;
	public static final JsonFieldType SIMPLE_STRING = JsonFieldType.STRING;
	public static final JsonFieldType SIMPLE_NUMBER = JsonFieldType.NUMBER;
	public static final JsonFieldType SIMPLE_ARRAY = JsonFieldType.ARRAY;
	public static final JsonFieldType SIMPLE_OBJECT = JsonFieldType.OBJECT;
	public static final JsonFieldType SIMPLE_NULL = JsonFieldType.NULL;

}