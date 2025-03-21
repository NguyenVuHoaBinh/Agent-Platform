package viettel.dac.promptservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:prompt-service}")
    private String applicationName;

    @Value("${api.version:1.0.0}")
    private String apiVersion;

    @Value("${springdoc.server.url:http://localhost:8082}")
    private String serverUrl;

    @Bean
    public OpenAPI promptServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url(serverUrl).description("Default Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    private Info apiInfo() {
        return new Info()
                .title(applicationName + " API")
                .description("REST API for " + applicationName)
                .version(apiVersion)
                .contact(new Contact()
                        .name("AgentFlow Development Team")
                        .email("agentflow@viettel.com.vn")
                        .url("https://agentflow.viettel.com.vn"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://agentflow.viettel.com.vn/license"));
    }
}