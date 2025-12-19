package com.tripdeva.restdocs.generator;

import com.tripdeva.restdocs.SupportRestDocsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * OpenAPI 3.0 JSON 생성을 위한 실행 클래스
 * Gradle JavaExec 태스크에서 호출됩니다.
 */
public class OpenApiGeneratorRunner {

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException("Usage: OpenApiGeneratorRunner <outputDir>");
		}

		String outputDir = args[0];

		System.out.println("Starting OpenAPI 3.0 generation...");
		System.out.println("Output Directory: " + outputDir);

		// Spring Application Context 생성
		SpringApplication springApp = new SpringApplication(SupportRestDocsApplication.class);
		springApp.setWebApplicationType(WebApplicationType.NONE);

		ConfigurableApplicationContext context = springApp.run(args);

		try {
			// OpenAPI 3.0 생성기 실행 - snippet 경로는 OpenApiProperties에서 자동 주입
			OpenApi3Generator generator = context.getBean(OpenApi3Generator.class);
			generator.generateOpenApiJson(null, outputDir);

			// Request Body to Parameters 변환 실행
			RequestBodyToParametersConverter converter = context.getBean(RequestBodyToParametersConverter.class);
			converter.convertRequestBodyToParameters(outputDir + "/openapi3.json");

			System.out.println("OpenAPI 3.0 JSON generation completed successfully!");

		} catch (Exception e) {
			System.err.println("Failed to generate OpenAPI 3.0 JSON: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} finally {
			context.close();
		}
	}
}