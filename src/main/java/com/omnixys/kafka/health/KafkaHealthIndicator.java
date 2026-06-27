package com.omnixys.kafka.health;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaHealthIndicator implements HealthIndicator {

    private final String bootstrapServers;

    public KafkaHealthIndicator(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public Health health() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

        long start = System.currentTimeMillis();
        try (Admin admin = Admin.create(props)) {
            var desc = admin.describeCluster(new DescribeClusterOptions().timeoutMs(5000));
            var clusterId = desc.clusterId().get(5, TimeUnit.SECONDS);
            var nodes = desc.nodes().get(5, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - start;
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("brokerCount", nodes.size())
                    .withDetail("latencyMs", latency)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return Health.down(e)
                    .withDetail("latencyMs", latency)
                    .build();
        }
    }
}
