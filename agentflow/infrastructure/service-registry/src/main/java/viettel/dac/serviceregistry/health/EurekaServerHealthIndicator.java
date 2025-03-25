package viettel.dac.serviceregistry.health;

import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Eureka Server status
 * Provides additional information about the Eureka server's state
 */
@Component
public class EurekaServerHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();

            // Get the server configuration
            boolean selfPreservationMode = serverContext.getServerConfig().shouldEnableSelfPreservation();
            int registryCount = serverContext.getRegistry().getApplications().size();
            int instanceCount = serverContext.getRegistry().getApplications().getRegisteredApplications().stream()
                    .mapToInt(app -> app.getInstances().size())
                    .sum();

            return Health.up()
                    .withDetail("selfPreservationMode", selfPreservationMode)
                    .withDetail("registeredApplications", registryCount)
                    .withDetail("registeredInstances", instanceCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Failed to access Eureka server context: " + e.getMessage())
                    .build();
        }
    }
}