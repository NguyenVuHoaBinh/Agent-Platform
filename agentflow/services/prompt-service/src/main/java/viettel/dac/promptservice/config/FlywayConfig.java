package viettel.dac.promptservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for Flyway database migrations.
 * Provides customized migration strategies for different environments.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Development environment migration strategy.
     * Performs a clean operation if validation fails.
     */
    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy developmentMigrationStrategy() {
        return flyway -> {
            try {
                flyway.validate();
                log.info("Flyway validation successful, executing migration");
            } catch (Exception e) {
                log.warn("Flyway validation failed: {}. Cleaning database before migration", e.getMessage());
                flyway.clean();
            }
            flyway.migrate();
            log.info("Flyway migration completed successfully");
        };
    }

    /**
     * Production environment migration strategy.
     * Applies strict validation and never cleans the database.
     */
    @Bean
    @Profile("prod")
    public FlywayMigrationStrategy productionMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway migration in production environment");
            // Run validation separately to get better error messages
            flyway.validate();
            flyway.migrate();
            log.info("Flyway migration completed successfully");
        };
    }

    /**
     * Default migration strategy for any other environment.
     */
    @Bean
    @Profile("!dev & !prod")
    public FlywayMigrationStrategy defaultMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway migration with default strategy");
            flyway.migrate();
            log.info("Flyway migration completed successfully");
        };
    }
}