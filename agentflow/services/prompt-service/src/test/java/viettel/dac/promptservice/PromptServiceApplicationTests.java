package viettel.dac.promptservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "spring.elasticsearch.enabled=false",
        "spring.data.elasticsearch.repositories.enabled=false",
        "spring.flyway.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {PromptServiceApplicationTests.TestConfig.class}
)
@ActiveProfiles("test")
class PromptServiceApplicationTests {

    @Configuration
    @EnableAutoConfiguration(exclude = {
        ElasticsearchDataAutoConfiguration.class, 
        ElasticsearchRepositoriesAutoConfiguration.class
    })
    static class TestConfig {
        // Empty test configuration
    }

    @Test
    void contextLoads() {
        // Simple test to check if application context loads
    }

}
