// src/main/java/viettel/dac/serviceregistry/metrics/EurekaMetricsCollector.java
package viettel.dac.serviceregistry.metrics;

import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EurekaMetricsCollector implements MeterBinder {

    private final Map<String, Long> lastUpdateTimestamps = new ConcurrentHashMap<>();

    @Override
    public void bindTo(MeterRegistry registry) {
        // Registry size metric
        Gauge.builder("eureka.registry.size", this, value -> getRegistrySize())
                .description("Number of applications in Eureka registry")
                .register(registry);

        // Registry instance count
        Gauge.builder("eureka.registry.instances", this, value -> getInstanceCount())
                .description("Total number of instances across all applications")
                .register(registry);

        // Application count by status
        Gauge.builder("eureka.registry.instances.up", this, value -> getInstanceCountByStatus("UP"))
                .description("Number of instances with UP status")
                .register(registry);

        Gauge.builder("eureka.registry.instances.down", this, value -> getInstanceCountByStatus("DOWN"))
                .description("Number of instances with DOWN status")
                .register(registry);

        // Self-preservation mode status
        Gauge.builder("eureka.server.self-preservation", this, value -> isSelfPreservationModeEnabled() ? 1 : 0)
                .description("Whether self-preservation mode is enabled (1) or disabled (0)")
                .register(registry);

        // Renewal threshold
        Gauge.builder("eureka.server.renewal.threshold", this, value -> getRenewalThreshold())
                .description("Current renewal threshold")
                .register(registry);

        // Last update timestamps for applications
        registerLastUpdateMetrics(registry);

        // Replication metrics
        Gauge.builder("eureka.registry.replication.events", this, value -> getReplicationEventCount())
                .description("Number of replication events")
                .register(registry);
    }

    private double getRegistrySize() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            return serverContext.getRegistry().getApplications().size();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getInstanceCount() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            return serverContext.getRegistry().getApplications().getRegisteredApplications().stream()
                    .mapToInt(app -> app.getInstances().size())
                    .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getInstanceCountByStatus(String status) {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            return serverContext.getRegistry().getApplications().getRegisteredApplications().stream()
                    .flatMap(app -> app.getInstances().stream())
                    .filter(instance -> status.equals(instance.getStatus().toString()))
                    .count();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean isSelfPreservationModeEnabled() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            return serverContext.getServerConfig().shouldEnableSelfPreservation();
        } catch (Exception e) {
            return false;
        }
    }

    private double getRenewalThreshold() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            return serverContext.getServerConfig().getRenewalPercentThreshold();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void registerLastUpdateMetrics(MeterRegistry registry) {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            List<Application> applications = serverContext.getRegistry().getApplications().getRegisteredApplications();

            for (Application app : applications) {
                final String appName = app.getName();

                Gauge.builder("eureka.registry.application.lastUpdate", this, value -> getLastUpdateTimestamp(appName))
                        .description("Last update timestamp for application")
                        .tag("application", appName)
                        .register(registry);

                // Update the timestamp
                lastUpdateTimestamps.put(appName, System.currentTimeMillis());
            }
        } catch (Exception e) {
            // Handle the exception
        }
    }

    private double getLastUpdateTimestamp(String appName) {
        return lastUpdateTimestamps.getOrDefault(appName, 0L);
    }

    private double getReplicationEventCount() {
        // In a real implementation, you would track replication events
        // This is a placeholder that would need to be implemented with actual replication tracking
        return 0.0;
    }
}