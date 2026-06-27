package com.omnixys.kafka.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "omnixys.kafka")
public class OmnixysKafkaProperties {

    private boolean enabled = true;

    private String bootstrapServers = "localhost:9092";
    private String clientId = "omnixys-client";
    private String groupId = "omnixys-group";

    private Map<String, String> topics = new HashMap<>();

    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private Dlq dlq = new Dlq();

    @Data
    public static class Producer {
        private int retries = 3;
        private String acks = "all";
    }

    @Data
    public static class Consumer {
        private int concurrency = 3;
        private boolean autoCommit = false;
    }

    @Data
    public static class Dlq {
        private boolean enabled = true;
        private String suffix = ".DLT";
        private int maxRetries = 3;
    }
}