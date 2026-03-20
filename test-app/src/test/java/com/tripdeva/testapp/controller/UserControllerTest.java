package com.tripdeva.testapp.controller;

import static com.tripdeva.restdocs.support.ResourceSnippet.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripdeva.restdocs.support.RestDocsTestSupport;
import com.tripdeva.testapp.dto.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserControllerTest extends RestDocsTestSupport {

	// ── POST /api/users → 201 Created + 커스텀 헤더 ──
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
								.description("새 사용자를 생성하고 201 Created를 반환합니다.")
								.requestHeaders(
										headerWithName("Content-Type").description("콘텐츠 타입")
								)
								.responseHeaders(
										headerWithName("X-Request-Id").description("요청 추적 ID")
								)
								.requestFields(
										fieldWithPath("name").type(STRING).description("이름"),
										fieldWithPath("email").type(STRING).description("이메일")
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data.id").type(NUMBER).description("사용자 ID"),
										fieldWithPath("data.name").type(STRING).description("이름"),
										fieldWithPath("data.email").type(STRING).description("이메일")
								)
								.build()
				));
	}

	// ── GET /api/users/{id} → 200 + path parameter ──
	@Test
	void getUser() throws Exception {
		mockMvc.perform(get("/api/users/{id}", 1L))
				.andExpect(status().isOk())
				.andDo(document("get-user",
						resource()
								.tag("User")
								.summary("사용자 조회")
								.pathParameters(
										parameterWithName("id").description("사용자 ID")
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data.id").type(NUMBER).description("사용자 ID"),
										fieldWithPath("data.name").type(STRING).description("이름"),
										fieldWithPath("data.email").type(STRING).description("이메일")
								)
								.build()
				));
	}

	// ── PUT /api/users/{id} → 200 + path param + request body ──
	@Test
	void updateUser() throws Exception {
		mockMvc.perform(put("/api/users/{id}", 1L)
						.contentType(APPLICATION_JSON)
						.content(toJson(new UserRequest("Jane", "jane@test.com"))))
				.andExpect(status().isOk())
				.andDo(document("update-user",
						resource()
								.tag("User")
								.summary("사용자 수정")
								.pathParameters(
										parameterWithName("id").description("사용자 ID")
								)
								.requestFields(
										fieldWithPath("name").type(STRING).description("이름"),
										fieldWithPath("email").type(STRING).description("이메일")
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data.id").type(NUMBER).description("사용자 ID"),
										fieldWithPath("data.name").type(STRING).description("이름"),
										fieldWithPath("data.email").type(STRING).description("이메일")
								)
								.build()
				));
	}

	// ── PATCH /api/users/{id} → 200 + optional fields ──
	@Test
	void patchUser() throws Exception {
		mockMvc.perform(patch("/api/users/{id}", 1L)
						.contentType(APPLICATION_JSON)
						.content(toJson(new UserRequest(null, "newemail@test.com"))))
				.andExpect(status().isOk())
				.andDo(document("patch-user",
						resource()
								.tag("User")
								.summary("사용자 부분 수정")
								.pathParameters(
										parameterWithName("id").description("사용자 ID")
								)
								.requestFields(
										fieldWithPath("name").type(STRING).description("이름").optional(),
										fieldWithPath("email").type(STRING).description("이메일").optional()
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data.id").type(NUMBER).description("사용자 ID"),
										fieldWithPath("data.name").type(STRING).description("이름"),
										fieldWithPath("data.email").type(STRING).description("이메일")
								)
								.build()
				));
	}

	// ── DELETE /api/users/{id} → 204 No Content ──
	@Test
	void deleteUser() throws Exception {
		mockMvc.perform(delete("/api/users/{id}", 1L))
				.andExpect(status().isNoContent())
				.andDo(document("delete-user",
						resource()
								.tag("User")
								.summary("사용자 삭제")
								.pathParameters(
										parameterWithName("id").description("사용자 ID")
								)
								.build()
				));
	}

	// ── GET /api/users/error-example → 404 에러 응답 ──
	@Test
	void errorExample() throws Exception {
		mockMvc.perform(get("/api/users/error-example"))
				.andExpect(status().isNotFound())
				.andDo(document("user-not-found",
						resource()
								.tag("User")
								.summary("사용자 없음 에러")
								.responseFields(
										fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
										fieldWithPath("code").type(STRING).description("에러 코드"),
										fieldWithPath("message").type(STRING).description("에러 메시지")
								)
								.build()
				));
	}
}
