package viettel.dac.serviceregistry.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * System resources health indicator
 * Monitors JVM and system resource metrics like memory usage and uptime
 */
@Component
public class SystemResourcesHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapUsagePercentage = (double) heapUsed / heapMax * 100;

        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();

        int availableProcessors = osMXBean.getAvailableProcessors();
        double systemLoad = osMXBean.getSystemLoadAverage();

        long uptime = runtimeMXBean.getUptime();

        // Consider the system unhealthy if heap usage is over 90%
        if (heapUsagePercentage > 90) {
            return Health.down()
                    .withDetail("reason", "High memory usage")
                    .withDetail("heapUsagePercentage", String.format("%.2f%%", heapUsagePercentage))
                    .withDetail("heapUsed", formatBytes(heapUsed))
                    .withDetail("heapMax", formatBytes(heapMax))
                    .withDetail("nonHeapUsed", formatBytes(nonHeapUsed))
                    .withDetail("availableProcessors", availableProcessors)
                    .withDetail("systemLoad", systemLoad)
                    .withDetail("uptime", formatUptime(uptime))
                    .build();
        }

        return Health.up()
                .withDetail("heapUsagePercentage", String.format("%.2f%%", heapUsagePercentage))
                .withDetail("heapUsed", formatBytes(heapUsed))
                .withDetail("heapMax", formatBytes(heapMax))
                .withDetail("nonHeapUsed", formatBytes(nonHeapUsed))
                .withDetail("availableProcessors", availableProcessors)
                .withDetail("systemLoad", systemLoad)
                .withDetail("uptime", formatUptime(uptime))
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours = hours % 24;
        minutes = minutes % 60;
        seconds = seconds % 60;

        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }
}