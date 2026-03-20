package com.tripdeva.restdocs.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * REST Docs 테스트를 위한 베이스 클래스.
 * MockMvc + REST Docs 자동 설정, prettyPrint 전처리기 기본 적용.
 */
@AutoConfigureMockMvc
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.example.com", uriPort = 443)
public abstract class RestDocsTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	protected String toJson(Object obj) throws JsonProcessingException {
		return objectMapper.writeValueAsString(obj);
	}
}
