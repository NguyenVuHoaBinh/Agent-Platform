package viettel.dac.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class RouteConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Identity Service Routes
                .route("identity-service-auth", r -> r
                        .path("/api/auth/**")
                        .uri("lb://identity-service"))
                .route("identity-service-users", r -> r
                        .path("/api/users/**")
                        .uri("lb://identity-service"))
                .route("identity-service-orgs", r -> r
                        .path("/api/organizations/**")
                        .uri("lb://identity-service"))

                // Prompt Service Routes
                .route("prompt-service", r -> r
                        .path("/api/prompts/**")
                        .uri("lb://prompt-service"))

                // Flow Service Routes
                .route("flow-service", r -> r
                        .path("/api/flows/**")
                        .uri("lb://flow-service"))

                // Agent Service Routes
                .route("agent-service", r -> r
                        .path("/api/agents/**")
                        .uri("lb://agent-service"))

                // Integration Service Routes
                .route("integration-service", r -> r
                        .path("/api/integrations/**")
                        .uri("lb://intergration-service"))

                // Add fallback route
                .route("fallback", r -> r
                        .path("/**")
                        .filters(f -> f.rewritePath("/(?<remaining>.*)", "/${remaining}")
                                .setStatus(404))
                        .uri("lb://api-gateway"))
                .build();
    }
}