package com.tripdeva.restdocs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenApiProperties.class)
@ComponentScan(basePackages = "com.tripdeva.restdocs")
public class SupportRestDocsConfig {
}
