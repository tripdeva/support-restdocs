# support-restdocs

Spring REST Docs 기반 OpenAPI 3.0 JSON 생성 + Swagger UI 통합 라이브러리.

테스트 코드에서 API 문서를 작성하면 OpenAPI 3.0 스펙을 자동 생성하고, Swagger UI로 바로 확인할 수 있다.

## 핵심 기능

- REST Docs 스니펫 → OpenAPI 3.0 JSON 자동 변환
- `import static ResourceSnippet.*` 하나로 대부분의 import 해결
- Swagger UI 기본 활성화 (설정으로 끄기 가능)
- Spring Boot AutoConfiguration 지원

## 빠른 시작

### 1. 의존성 추가

```groovy
// build.gradle
dependencies {
    testImplementation 'com.tripdeva:support-restdocs:0.0.1'
}
```

### 2. 테스트 코드 작성

```java
import static com.tripdeva.restdocs.support.ResourceSnippet.*;

@SpringBootTest
class UserControllerTest extends RestDocsTestSupport {

    @Test
    void createUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(APPLICATION_JSON)
                .content(toJson(new UserRequest("John", "john@test.com"))))
            .andExpect(status().isCreated())
            .andDo(document("create-user",
                resource()
                    .tag("User")
                    .summary("사용자 생성")
                    .requestHeaders(
                        headerWithName("Content-Type").description("콘텐츠 타입")
                    )
                    .requestFields(
                        fieldWithPath("name").type(STRING).description("이름"),
                        fieldWithPath("email").type(STRING).description("이메일")
                    )
                    .responseFields(
                        fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                        fieldWithPath("data.id").type(NUMBER).description("사용자 ID")
                    )
                    .build()
            ));
    }
}
```

### 3. OpenAPI JSON 생성

```bash
./gradlew openapi3
```

`build/api-spec/openapi3.json`에 OpenAPI 3.0 스펙이 생성된다.

### 4. Swagger UI 확인

서버 기동 후 http://localhost:8080/swagger-ui/index.html 접속.

## 설정

`application.yml`에서 설정한다.

```yaml
restdocs:
  title: My API                        # API 제목
  version: 1.0.0                       # API 버전
  openapi:
    output-file-name: openapi3.json    # 생성 파일명
    server:
      host: http://localhost
      port: "8080"
      context-path: ""                 # 컨텍스트 경로
    snippet-paths:                     # REST Docs 스니펫 경로
      - build/generated-snippets
    swagger:
      enabled: true                    # false → Swagger UI 비활성화
      output-dir: build/api-spec       # JSON 생성 디렉토리
```

### Swagger UI 끄기

```yaml
restdocs:
  openapi:
    swagger:
      enabled: false
```

`/swagger-ui/index.html`과 `/v3/api-docs` 엔드포인트가 모두 비활성화된다.

## ResourceSnippet API

`import static com.tripdeva.restdocs.support.ResourceSnippet.*` 하나로 아래 모두 사용 가능:

| 메서드 | 설명 |
|--------|------|
| `resource()` | ResourceBuilder 생성 |
| `fieldWithPath(path)` | 필드 디스크립터 |
| `subsectionWithPath(path)` | 서브섹션 디스크립터 |
| `headerWithName(name)` | 헤더 디스크립터 |
| `parameterWithName(name)` | 파라미터 디스크립터 (타입 지원) |
| `document(id, snippets...)` | REST Docs document |
| `document(id, builder)` | ResourceBuilder로 document |

### 타입 상수

`STRING`, `NUMBER`, `BOOLEAN`, `ARRAY`, `OBJECT`, `NULL`

### ResourceBuilder

```java
resource()
    .tag("User")                              // OpenAPI 태그
    .summary("사용자 생성")                     // 요약
    .description("상세 설명")                   // 설명
    .requestFields(...)                        // 요청 필드
    .responseFields(...)                       // 응답 필드
    .queryParameters(...)                      // 쿼리 파라미터
    .pathParameters(...)                       // 경로 파라미터
    .requestHeaders(...)                       // 요청 헤더
    .responseHeaders(...)                      // 응답 헤더
    .build()
```

## RestDocsTestSupport

테스트 베이스 클래스. `@SpringBootTest`와 함께 사용.

```java
@SpringBootTest
class MyTest extends RestDocsTestSupport {
    // mockMvc    — 자동 주입
    // objectMapper — 자동 주입
    // toJson(obj) — JSON 직렬화 헬퍼
}
```

## openapi3 Gradle Task

```groovy
// build.gradle
tasks.register('openapi3', JavaExec) {
    dependsOn 'test', 'classes'
    mainClass = 'com.tripdeva.restdocs.generator.OpenApiGeneratorRunner'
    classpath = sourceSets.main.runtimeClasspath + sourceSets.test.runtimeClasspath
    args project.file('build/api-spec').absolutePath
}
```

또는 Gradle 플러그인 적용:

```groovy
plugins {
    id 'com.tripdeva.openapi'
}
// openapi3 태스크가 자동 등록됨
```

## 생성되는 OpenAPI JSON 예시

```json
{
  "openapi": "3.0.0",
  "paths": {
    "/api/users": {
      "post": {
        "summary": "사용자 생성",
        "tags": ["User"],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": { "$ref": "#/components/schemas/create-userRequest" },
              "example": { "name": "John", "email": "john@test.com" }
            }
          }
        },
        "responses": {
          "201": {
            "description": "성공",
            "headers": {
              "X-Request-Id": { "description": "요청 추적 ID" }
            }
          }
        }
      }
    },
    "/api/users/{id}": {
      "get": { "..." : "..." },
      "put": { "..." : "..." },
      "patch": { "..." : "..." },
      "delete": {
        "responses": { "204": { "description": "성공" } }
      }
    }
  }
}
```

## 지원 항목

| 항목 | 지원 |
|------|------|
| HTTP 메서드 | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
| 상태코드 | http-response.adoc에서 자동 추출 (200, 201, 204, 4xx, 5xx) |
| Path Parameter | 자동 템플릿화 (`/users/1` → `/users/{id}`) |
| Query Parameter | query-parameters.adoc / request-parameters.adoc |
| Request Body | request-fields.adoc + example 자동 추출 |
| Response Body | response-fields.adoc |
| Request Header | request-headers.adoc |
| Response Header | response-headers.adoc |
| Optional 필드 | `.optional()` → required 배열에서 제외 |
| Metadata | tag, summary, description (metadata.yml) |
| 멀티라인 JSON | pretty-printed request body 파싱 |
| Spring REST Docs 3.x | query-parameters.adoc 2컬럼 포맷 지원 |

## 프로젝트 구조

```
support-restdocs/
├── src/main/java/com/tripdeva/restdocs/
│   ├── config/
│   │   ├── OpenApiProperties.java        # 설정 프로퍼티
│   │   ├── SupportRestDocsConfig.java     # 메인 Configuration
│   │   ├── SwaggerUiConfig.java           # Swagger 활성화
│   │   └── SwaggerUiDisabledConfig.java   # Swagger 비활성화
│   ├── generator/
│   │   ├── OpenApi3Generator.java         # 스니펫→OpenAPI 변환
│   │   ├── OpenApiGeneratorRunner.java    # Gradle task 실행기
│   │   └── RequestBodyToParametersConverter.java
│   ├── support/
│   │   ├── ResourceSnippet.java           # Builder + static delegates
│   │   └── RestDocsTestSupport.java       # 테스트 베이스 클래스
│   └── gradle/
│       └── OpenApiPlugin.java             # Gradle 플러그인
├── src/main/resources/
│   └── META-INF/spring/...imports         # AutoConfiguration
├── test-app/                              # 검증용 테스트 앱
└── build.gradle
```

## 기술 스택

- Java 17+
- Spring Boot 3.2+
- Spring REST Docs 3.x
- springdoc-openapi 2.7
- Gradle 8.x

## 라이선스

MIT
