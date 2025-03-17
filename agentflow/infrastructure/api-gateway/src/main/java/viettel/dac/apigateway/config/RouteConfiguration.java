package viettel.dac.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import viettel.dac.apigateway.filter.JwtAuthenticationFilter;

@Configuration
public class RouteConfiguration {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Identity Service Routes
                .route("identity-service-auth", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/auth/(?<segment>.*)", "/api/auth/${segment}"))
                        .uri("lb://identity-service"))

                .route("identity-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/users/(?<segment>.*)", "/api/users/${segment}"))
                        .uri("lb://identity-service"))

                .route("identity-service-orgs", r -> r
                        .path("/api/organizations/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/organizations/(?<segment>.*)", "/api/organizations/${segment}"))
                        .uri("lb://identity-service"))

                .route("identity-service-projects", r -> r
                        .path("/api/projects/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/projects/(?<segment>.*)", "/api/projects/${segment}"))
                        .uri("lb://identity-service"))

                // Prompt Service Routes
                .route("prompt-service", r -> r
                        .path("/api/prompts/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/prompts/(?<segment>.*)", "/api/prompts/${segment}"))
                        .uri("lb://prompt-service"))

                // Flow Service Routes
                .route("flow-service", r -> r
                        .path("/api/flows/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/flows/(?<segment>.*)", "/api/flows/${segment}"))
                        .uri("lb://flow-service"))

                // Agent Service Routes
                .route("agent-service", r -> r
                        .path("/api/agents/**", "/api/conversations/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/(agents|conversations)/(?<segment>.*)", "/api/$1/${segment}"))
                        .uri("lb://agent-service"))

                // Integration Service Routes
                .route("integration-service", r -> r
                        .path("/api/integrations/**", "/api/webhooks/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .rewritePath("/api/(integrations|webhooks)/(?<segment>.*)", "/api/$1/${segment}"))
                        .uri("lb://integration-service"))

                // Actuator Routes
                .route("actuator", r -> r
                        .path("/actuator/**")
                        .uri("lb://api-gateway"))

                // Add fallback route
                .route("fallback", r -> r
                        .path("/**")
                        .filters(f -> f.setStatus(404))
                        .uri("lb://api-gateway"))
                .build();
    }
}