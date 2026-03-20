package com.tripdeva.restdocs.support;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.snippet.Snippet;

import static org.junit.jupiter.api.Assertions.*;

class ResourceSnippetTest {

    // ═══════════════════════════════════════════════════════════════
    // resource() — 팩토리 메서드
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("resource(): ResourceBuilder 인스턴스 반환")
    void resource_returnsBuilder() {
        ResourceSnippet.ResourceBuilder builder = ResourceSnippet.resource();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("resource(): 매번 새 인스턴스")
    void resource_newInstanceEachTime() {
        ResourceSnippet.ResourceBuilder a = ResourceSnippet.resource();
        ResourceSnippet.ResourceBuilder b = ResourceSnippet.resource();
        assertNotSame(a, b);
    }

    // ═══════════════════════════════════════════════════════════════
    // schema() — 스키마 팩토리
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("schema(): 이름이 설정된다")
    void schema_name() {
        ResourceSnippet.Schema schema = ResourceSnippet.schema("UserRequest");
        assertEquals("UserRequest", schema.name());
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — 체이닝
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("tag(): 태그 설정 및 getter")
    void builder_tag() {
        var builder = ResourceSnippet.resource().tag("User");
        assertEquals("User", builder.getTag());
    }

    @Test
    @DisplayName("summary(): 요약 설정 및 getter")
    void builder_summary() {
        var builder = ResourceSnippet.resource().summary("사용자 생성");
        assertEquals("사용자 생성", builder.getSummary());
    }

    @Test
    @DisplayName("description(): 설명 설정 및 getter")
    void builder_description() {
        var builder = ResourceSnippet.resource().description("상세 설명");
        assertEquals("상세 설명", builder.getDescription());
    }

    @Test
    @DisplayName("requestSchema(): 스키마 설정")
    void builder_requestSchema() {
        var schema = ResourceSnippet.schema("Req");
        var builder = ResourceSnippet.resource().requestSchema(schema);
        assertEquals("Req", builder.getRequestSchema().name());
    }

    @Test
    @DisplayName("responseSchema(): 스키마 설정")
    void builder_responseSchema() {
        var schema = ResourceSnippet.schema("Resp");
        var builder = ResourceSnippet.resource().responseSchema(schema);
        assertEquals("Resp", builder.getResponseSchema().name());
    }

    @Test
    @DisplayName("체이닝이 동일 인스턴스를 반환한다")
    void builder_chaining_sameInstance() {
        var builder = ResourceSnippet.resource();
        var result = builder.tag("T").summary("S").description("D");
        assertSame(builder, result);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — requestFields
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("requestFields(): 필드 설정 및 getter")
    void builder_requestFields() {
        FieldDescriptor fd = ResourceSnippet.fieldWithPath("name").type(JsonFieldType.STRING).description("이름");
        var builder = ResourceSnippet.resource().requestFields(fd);
        assertEquals(1, builder.getRequestFields().length);
        assertEquals("name", builder.getRequestFields()[0].getPath());
    }

    @Test
    @DisplayName("requestFields(): 다수 필드")
    void builder_requestFields_multiple() {
        var builder = ResourceSnippet.resource().requestFields(
                ResourceSnippet.fieldWithPath("a").description("A"),
                ResourceSnippet.fieldWithPath("b").description("B"),
                ResourceSnippet.fieldWithPath("c").description("C")
        );
        assertEquals(3, builder.getRequestFields().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — responseFields
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("responseFields(): 필드 설정")
    void builder_responseFields() {
        var builder = ResourceSnippet.resource().responseFields(
                ResourceSnippet.fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공")
        );
        assertEquals(1, builder.getResponseFields().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — queryParameters (ParameterDescriptor)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("queryParameters(ParameterDescriptor...): 설정")
    void builder_queryParameters_paramDescriptor() {
        ParameterDescriptor pd = org.springframework.restdocs.request.RequestDocumentation
                .parameterWithName("page").description("페이지");
        var builder = ResourceSnippet.resource().queryParameters(pd);
        assertEquals(1, builder.getQueryParameters().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — queryParameters (TypedParameterDescriptor)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("queryParameters(TypedParameterDescriptor...): 설정 및 변환")
    void builder_queryParameters_typedDescriptor() {
        var typed = ResourceSnippet.parameterWithName("size")
                .type(JsonFieldType.NUMBER)
                .description("크기")
                .optional();
        var builder = ResourceSnippet.resource().queryParameters(typed);
        assertEquals(1, builder.getQueryParameters().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — pathParameters
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("pathParameters(ParameterDescriptor...): 설정")
    void builder_pathParameters_paramDescriptor() {
        ParameterDescriptor pd = org.springframework.restdocs.request.RequestDocumentation
                .parameterWithName("id").description("ID");
        var builder = ResourceSnippet.resource().pathParameters(pd);
        assertEquals(1, builder.getPathParameters().length);
    }

    @Test
    @DisplayName("pathParameters(TypedParameterDescriptor...): 변환")
    void builder_pathParameters_typedDescriptor() {
        var typed = ResourceSnippet.parameterWithName("id").description("ID");
        var builder = ResourceSnippet.resource().pathParameters(typed);
        assertEquals(1, builder.getPathParameters().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder — headers
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("requestHeaders(): 헤더 설정")
    void builder_requestHeaders() {
        HeaderDescriptor hd = ResourceSnippet.headerWithName("Authorization").description("인증");
        var builder = ResourceSnippet.resource().requestHeaders(hd);
        assertEquals(1, builder.getRequestHeaders().length);
    }

    @Test
    @DisplayName("responseHeaders(): 헤더 설정")
    void builder_responseHeaders() {
        var builder = ResourceSnippet.resource().responseHeaders(
                ResourceSnippet.headerWithName("X-Id").description("ID"),
                ResourceSnippet.headerWithName("X-Rate").description("Rate")
        );
        assertEquals(2, builder.getResponseHeaders().length);
    }

    // ═══════════════════════════════════════════════════════════════
    // ResourceBuilder.build() — Snippet 배열 생성
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("build(): 아무것도 설정하지 않으면 빈 배열")
    void build_empty() {
        Snippet[] snippets = ResourceSnippet.resource().build();
        assertEquals(0, snippets.length);
    }

    @Test
    @DisplayName("build(): tag만 설정하면 MetadataSnippet 1개")
    void build_tagOnly() {
        Snippet[] snippets = ResourceSnippet.resource().tag("User").build();
        assertEquals(1, snippets.length);
        assertInstanceOf(ResourceSnippet.MetadataSnippet.class, snippets[0]);
    }

    @Test
    @DisplayName("build(): summary만 설정해도 MetadataSnippet")
    void build_summaryOnly() {
        Snippet[] snippets = ResourceSnippet.resource().summary("test").build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): description만 설정해도 MetadataSnippet")
    void build_descriptionOnly() {
        Snippet[] snippets = ResourceSnippet.resource().description("desc").build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): requestFields → 2개 snippet (requestFields + OptionalFieldsSnippet)")
    void build_requestFields_twoSnippets() {
        Snippet[] snippets = ResourceSnippet.resource()
                .requestFields(ResourceSnippet.fieldWithPath("a").description("A"))
                .build();
        assertEquals(2, snippets.length); // requestFields + OptionalFieldsSnippet
    }

    @Test
    @DisplayName("build(): responseFields → 1개 snippet")
    void build_responseFields_oneSnippet() {
        Snippet[] snippets = ResourceSnippet.resource()
                .responseFields(ResourceSnippet.fieldWithPath("b").description("B"))
                .build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): queryParameters → 1개 snippet")
    void build_queryParams_oneSnippet() {
        Snippet[] snippets = ResourceSnippet.resource()
                .queryParameters(ResourceSnippet.parameterWithName("q").description("Q"))
                .build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): pathParameters → 1개 snippet")
    void build_pathParams_oneSnippet() {
        Snippet[] snippets = ResourceSnippet.resource()
                .pathParameters(ResourceSnippet.parameterWithName("id").description("ID"))
                .build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): requestHeaders → 1개 snippet")
    void build_requestHeaders_oneSnippet() {
        Snippet[] snippets = ResourceSnippet.resource()
                .requestHeaders(ResourceSnippet.headerWithName("Auth").description("인증"))
                .build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): responseHeaders → 1개 snippet")
    void build_responseHeaders_oneSnippet() {
        Snippet[] snippets = ResourceSnippet.resource()
                .responseHeaders(ResourceSnippet.headerWithName("X-Id").description("ID"))
                .build();
        assertEquals(1, snippets.length);
    }

    @Test
    @DisplayName("build(): 전체 조합 → 모든 snippet 포함")
    void build_fullCombination() {
        Snippet[] snippets = ResourceSnippet.resource()
                .tag("User")
                .summary("생성")
                .description("설명")
                .requestFields(ResourceSnippet.fieldWithPath("name").description("이름"))
                .responseFields(ResourceSnippet.fieldWithPath("id").description("ID"))
                .queryParameters(ResourceSnippet.parameterWithName("q").description("Q"))
                .pathParameters(ResourceSnippet.parameterWithName("id").description("ID"))
                .requestHeaders(ResourceSnippet.headerWithName("Auth").description("인증"))
                .responseHeaders(ResourceSnippet.headerWithName("X-Id").description("ID"))
                .build();
        // metadata(1) + requestFields(1) + optionalFields(1) + responseFields(1)
        // + queryParams(1) + pathParams(1) + reqHeaders(1) + respHeaders(1) = 8
        assertEquals(8, snippets.length);
    }

    @Test
    @DisplayName("build(): 빈 배열 필드는 snippet 생성 안 함")
    void build_emptyArrays_noSnippets() {
        Snippet[] snippets = ResourceSnippet.resource()
                .requestFields()
                .responseFields()
                .build();
        assertEquals(0, snippets.length);
    }

    // ═══════════════════════════════════════════════════════════════
    // TypedParameterDescriptor
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TypedParameterDescriptor: type 설정")
    void typedParam_type() {
        var typed = ResourceSnippet.parameterWithName("page");
        typed.type(JsonFieldType.NUMBER);
        assertEquals(JsonFieldType.NUMBER, typed.getType());
    }

    @Test
    @DisplayName("TypedParameterDescriptor: description 설정")
    void typedParam_description() {
        var typed = ResourceSnippet.parameterWithName("page").description("페이지");
        assertNotNull(typed.getDelegate());
    }

    @Test
    @DisplayName("TypedParameterDescriptor: optional 설정")
    void typedParam_optional() {
        var typed = ResourceSnippet.parameterWithName("page").optional();
        assertTrue(typed.getDelegate().isOptional());
    }

    @Test
    @DisplayName("TypedParameterDescriptor: defaultValue 설정")
    void typedParam_defaultValue() {
        var typed = ResourceSnippet.parameterWithName("page").defaultValue("0");
        assertNotNull(typed.getDelegate());
    }

    @Test
    @DisplayName("TypedParameterDescriptor: 체이닝")
    void typedParam_chaining() {
        var typed = ResourceSnippet.parameterWithName("size")
                .type(JsonFieldType.NUMBER)
                .description("크기")
                .optional()
                .defaultValue("10");
        assertEquals(JsonFieldType.NUMBER, typed.getType());
        assertTrue(typed.getDelegate().isOptional());
    }

    @Test
    @DisplayName("TypedParameterDescriptor: getDelegate()는 ParameterDescriptor 반환")
    void typedParam_getDelegate() {
        var typed = ResourceSnippet.parameterWithName("test").description("T");
        ParameterDescriptor delegate = typed.getDelegate();
        assertNotNull(delegate);
        assertEquals("test", delegate.getName());
    }

    // ═══════════════════════════════════════════════════════════════
    // Static delegate 메서드
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("fieldWithPath(): PayloadDocumentation 위임")
    void fieldWithPath_delegates() {
        FieldDescriptor fd = ResourceSnippet.fieldWithPath("name");
        assertNotNull(fd);
        assertEquals("name", fd.getPath());
    }

    @Test
    @DisplayName("fieldWithPath(): type 체이닝")
    void fieldWithPath_withType() {
        FieldDescriptor fd = ResourceSnippet.fieldWithPath("age").type(JsonFieldType.NUMBER);
        assertNotNull(fd);
    }

    @Test
    @DisplayName("subsectionWithPath(): 위임")
    void subsectionWithPath_delegates() {
        FieldDescriptor fd = ResourceSnippet.subsectionWithPath("data");
        assertNotNull(fd);
    }

    @Test
    @DisplayName("headerWithName(): HeaderDocumentation 위임")
    void headerWithName_delegates() {
        HeaderDescriptor hd = ResourceSnippet.headerWithName("X-Custom");
        assertNotNull(hd);
        assertEquals("X-Custom", hd.getName());
    }

    @Test
    @DisplayName("document(String, Snippet...): 위임")
    void document_snippets() {
        // document()가 null을 반환하지 않는지만 검증 (실제 동작은 통합 테스트)
        var handler = ResourceSnippet.document("test-id");
        assertNotNull(handler);
    }

    @Test
    @DisplayName("document(String, ResourceBuilder): 위임")
    void document_resourceBuilder() {
        var builder = ResourceSnippet.resource().tag("Test").summary("Test");
        var handler = ResourceSnippet.document("test-id", builder);
        assertNotNull(handler);
    }

    // ═══════════════════════════════════════════════════════════════
    // JsonFieldType 상수
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BOOLEAN 상수")
    void constant_boolean() { assertEquals(JsonFieldType.BOOLEAN, ResourceSnippet.BOOLEAN); }

    @Test
    @DisplayName("STRING 상수")
    void constant_string() { assertEquals(JsonFieldType.STRING, ResourceSnippet.STRING); }

    @Test
    @DisplayName("NUMBER 상수")
    void constant_number() { assertEquals(JsonFieldType.NUMBER, ResourceSnippet.NUMBER); }

    @Test
    @DisplayName("ARRAY 상수")
    void constant_array() { assertEquals(JsonFieldType.ARRAY, ResourceSnippet.ARRAY); }

    @Test
    @DisplayName("OBJECT 상수")
    void constant_object() { assertEquals(JsonFieldType.OBJECT, ResourceSnippet.OBJECT); }

    @Test
    @DisplayName("NULL 상수")
    void constant_null() { assertEquals(JsonFieldType.NULL, ResourceSnippet.NULL); }

    @Test
    @DisplayName("SIMPLE_* 상수는 원본과 동일")
    void constant_simpleAliases() {
        assertSame(ResourceSnippet.BOOLEAN, ResourceSnippet.SIMPLE_BOOLEAN);
        assertSame(ResourceSnippet.STRING, ResourceSnippet.SIMPLE_STRING);
        assertSame(ResourceSnippet.NUMBER, ResourceSnippet.SIMPLE_NUMBER);
        assertSame(ResourceSnippet.ARRAY, ResourceSnippet.SIMPLE_ARRAY);
        assertSame(ResourceSnippet.OBJECT, ResourceSnippet.SIMPLE_OBJECT);
        assertSame(ResourceSnippet.NULL, ResourceSnippet.SIMPLE_NULL);
    }

    // ═══════════════════════════════════════════════════════════════
    // MetadataSnippet
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("MetadataSnippet: record 접근자")
    void metadataSnippet_accessors() {
        var ms = new ResourceSnippet.MetadataSnippet("Tag", "Summary", "Description");
        assertEquals("Tag", ms.tag());
        assertEquals("Summary", ms.summary());
        assertEquals("Description", ms.description());
    }

    @Test
    @DisplayName("MetadataSnippet: null 값 허용")
    void metadataSnippet_nullValues() {
        var ms = new ResourceSnippet.MetadataSnippet(null, null, null);
        assertNull(ms.tag());
        assertNull(ms.summary());
        assertNull(ms.description());
    }

    // ═══════════════════════════════════════════════════════════════
    // Schema
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Schema: record name 접근")
    void schema_name_accessor() {
        var schema = new ResourceSnippet.Schema("TestSchema");
        assertEquals("TestSchema", schema.name());
    }

    @Test
    @DisplayName("Schema: equals/hashCode")
    void schema_equalsHashCode() {
        var a = new ResourceSnippet.Schema("S1");
        var b = new ResourceSnippet.Schema("S1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
