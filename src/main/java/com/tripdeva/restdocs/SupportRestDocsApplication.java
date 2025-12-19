package com.tripdeva.restdocs;

import com.tripdeva.restdocs.config.SupportRestDocsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * REST Docs 독립 실행을 위한 Application 클래스
 * OpenApiGeneratorRunner에서만 사용됩니다.
 */
@EnableConfigurationProperties
@Import(SupportRestDocsConfig.class)
@SpringBootApplication(
		scanBasePackages = "com.tripdeva.restdocs",
		exclude = {
				DataSourceAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class
		}
)
public class SupportRestDocsApplication {
}