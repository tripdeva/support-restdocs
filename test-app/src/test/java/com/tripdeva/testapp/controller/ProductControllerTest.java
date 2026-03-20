package com.tripdeva.testapp.controller;

import static com.tripdeva.restdocs.support.ResourceSnippet.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripdeva.restdocs.support.RestDocsTestSupport;
import com.tripdeva.testapp.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductControllerTest extends RestDocsTestSupport {

	// ── GET /api/products?page=&size= → 200 + query parameters ──
	@Test
	void listProducts() throws Exception {
		mockMvc.perform(get("/api/products")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andDo(document("list-products",
						resource()
								.tag("Product")
								.summary("상품 목록 조회")
								.queryParameters(
										parameterWithName("page").type(NUMBER).description("페이지 번호").optional(),
										parameterWithName("size").type(NUMBER).description("페이지 크기").optional()
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data[]").type(ARRAY).description("상품 목록"),
										fieldWithPath("data[].id").type(NUMBER).description("상품 ID"),
										fieldWithPath("data[].name").type(STRING).description("상품명"),
										fieldWithPath("data[].price").type(NUMBER).description("가격")
								)
								.build()
				));
	}

	// ── POST /api/products → 200 + request body with integer field ──
	@Test
	void createProduct() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(APPLICATION_JSON)
						.content(toJson(new ProductRequest("Laptop", 1200))))
				.andExpect(status().isOk())
				.andDo(document("create-product",
						resource()
								.tag("Product")
								.summary("상품 생성")
								.requestFields(
										fieldWithPath("name").type(STRING).description("상품명"),
										fieldWithPath("price").type(NUMBER).description("가격")
								)
								.responseFields(
										fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
										fieldWithPath("data.id").type(NUMBER).description("상품 ID"),
										fieldWithPath("data.name").type(STRING).description("상품명"),
										fieldWithPath("data.price").type(NUMBER).description("가격")
								)
								.build()
				));
	}
}
