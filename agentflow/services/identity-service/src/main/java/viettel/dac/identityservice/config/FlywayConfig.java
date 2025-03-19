package viettel.dac.identityservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Custom Flyway migration strategy for development environment.
     * This will drop the database and recreate it before running migrations.
     * Should only be used in development environments.
     */
    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            log.warn("Cleaning database before migration (dev profile)");
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * Custom Flyway migration strategy for test environment.
     * This will drop the database and recreate it before running migrations.
     * Should only be used in test environments.
     */
    @Bean
    @Profile("test")
    public FlywayMigrationStrategy cleanMigrateTestStrategy() {
        return flyway -> {
            log.warn("Cleaning database before migration (test profile)");
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * Default Flyway migration strategy for all other environments.
     * This will only run migrations and not clean the database.
     */
    @Bean
    @Profile("!dev & !test")
    public FlywayMigrationStrategy migrateStrategy() {
        return Flyway::migrate;
    }
}