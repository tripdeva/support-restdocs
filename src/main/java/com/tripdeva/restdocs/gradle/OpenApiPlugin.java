package com.tripdeva.restdocs.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.JavaExec;

/**
 * OpenAPI 자동 생성을 위한 Gradle 플러그인
 * 이 플러그인을 적용하면 자동으로 openapi3 태스크가 추가됩니다.
 */
public class OpenApiPlugin implements Plugin<Project> {
    
    @Override
    public void apply(Project project) {
        // Java 플러그인이 적용되어 있는지 확인
        project.getPlugins().apply(JavaPlugin.class);
        
        System.out.println("OpenAPI Plugin이 " + project.getName() + " 프로젝트에 적용되었습니다.");
        
        // openapi3 태스크 등록
        project.getTasks().register("openapi3", JavaExec.class, task -> {
            task.setDescription("REST Docs 스니펫을 기반으로 OpenAPI 3.0 JSON 생성 (자동 생성)");
            task.setGroup("documentation");
            
            // REST Docs 테스트에 의존
            if (project.getTasks().findByName("restDocsTest") != null) {
                task.dependsOn("restDocsTest");
            } else {
                task.dependsOn("test");
            }
            task.dependsOn("classes");
            
            // 메인 클래스 설정
            task.getMainClass().set("com.tripdeva.restdocs.generator.OpenApiGeneratorRunner");
            
            // 클래스패스 설정
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            task.setClasspath(
                sourceSets.getByName("main").getRuntimeClasspath()
                .plus(sourceSets.getByName("test").getRuntimeClasspath())
            );
            
            // 출력 디렉토리 설정
            task.args(project.file("build/api-spec").getAbsolutePath());
            task.getOutputs().dir(project.file("build/api-spec"));
            
            // Spring 프로파일 시스템 프로퍼티 전달
            task.systemProperty("spring.profiles.active", 
                System.getProperty("spring.profiles.active", "local"));
        });
        
        // bootJar, bootRun 태스크가 있으면 openapi3에 의존하도록 설정
        project.afterEvaluate(proj -> {
            if (proj.getTasks().findByName("bootJar") != null) {
                proj.getTasks().named("bootJar").configure(task -> {
                    task.dependsOn("openapi3");
                });
                System.out.println("bootJar 태스크가 openapi3에 의존하도록 설정되었습니다.");
            }
            if (proj.getTasks().findByName("bootRun") != null) {
                proj.getTasks().named("bootRun").configure(task -> {
                    task.dependsOn("openapi3");
                });
                System.out.println("bootRun 태스크가 openapi3에 의존하도록 설정되었습니다.");
            }
        });
    }
}