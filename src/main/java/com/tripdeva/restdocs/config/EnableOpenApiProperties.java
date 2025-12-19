package com.tripdeva.restdocs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OpenAPI Properties를 활성화하는 어노테이션
 * 이 어노테이션을 사용하면 OpenApiProperties가 자동으로 로드됩니다.
 * 
 * Usage:
 * <pre>
 * &#64;Configuration
 * &#64;EnableOpenApiProperties
 * public class MyConfig {
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableConfigurationProperties(OpenApiProperties.class)
@Import({SupportRestDocsConfig.class, OpenApiWebConfig.class})
public @interface EnableOpenApiProperties {
}