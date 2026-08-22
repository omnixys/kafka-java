package com.omnixys.kafka.consumer;

import com.omnixys.context.ContextAccessor;
import com.omnixys.kafka.config.OmnixysKafkaProperties;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.model.EventType;
import com.omnixys.kafka.model.KafkaEnvelope;
import io.opentelemetry.api.OpenTelemetry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaConsumerServiceTest {

    private final KafkaEventDispatcher dispatcher = new KafkaEventDispatcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final OmnixysKafkaProperties properties = new OmnixysKafkaProperties();
    private final RecordingDedup dedup = new RecordingDedup();

    private KafkaConsumerService service;
    private RecordingHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        properties.setGroupId("test-group");
        service = new KafkaConsumerService(dispatcher, mapper, properties,
                OpenTelemetry.noop(), dedup);
        handler = new RecordingHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class, Map.class));
    }

    @Test
    void consume_shouldDispatchEnvelopeAndMarkProcessed() throws Exception {
        KafkaEnvelope<Map<String, String>> envelope =
                KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));
        String json = mapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", json);
        addContextHeaders(record);

        service.consume(record);

        assertThat(handler.envelope).isNotNull();
        assertThat(handler.envelope.eventId()).isEqualTo(envelope.eventId());
        assertThat(handler.envelope.eventName()).isEqualTo("order.created");
        assertThat(handler.envelope.eventType()).isEqualTo(EventType.EVENT);
        assertThat(handler.headers).containsKeys("x-tenant-id", "x-actor-id");
        assertThat(dedup.marked).containsExactly(envelope.eventId());
        assertThat(ContextAccessor.get()).isNull();
    }

    @Test
    void consume_shouldRebuildContextFromHeaders() throws Exception {
        KafkaEnvelope<Map<String, String>> envelope =
                KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));
        String json = mapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", json);
        addContextHeaders(record);

        service.consume(record);

        assertThat(handler.observedContext).isNotNull();
        assertThat(handler.observedContext.requestId()).isEqualTo("req-1");
        assertThat(handler.observedContext.correlationId()).isEqualTo("corr-1");
        assertThat(handler.observedContext.tenant()).isNotNull();
        assertThat(handler.observedContext.tenant().tenantId()).isEqualTo("tenant-a");
        assertThat(handler.observedContext.tenant().source()).isEqualTo("kafka");
        assertThat(handler.observedContext.principal().actorId()).isEqualTo("actor-9");
        assertThat(handler.observedContext.transport().topic()).isEqualTo("orders");
        assertThat(handler.observedContext.transport().partition()).isEqualTo(0);
        assertThat(handler.observedContext.transport().offset()).isEqualTo("5");
        assertThat(handler.observedContext.transport().consumerGroup()).isEqualTo("test-group");
        assertThat(ContextAccessor.get()).isNull();
    }

    @Test
    void consume_shouldFallBackToMetaHeadersForTenantAndActor() throws Exception {
        KafkaEnvelope<Map<String, String>> envelope =
                KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));
        String json = mapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", json);
        record.headers().add("x-request-id", "req-1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-correlation-id", "corr-1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-meta-tenantId", "tenant-meta".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-meta-actorId", "actor-meta".getBytes(StandardCharsets.UTF_8));

        service.consume(record);

        assertThat(handler.observedContext).isNotNull();
        assertThat(handler.observedContext.tenant().tenantId()).isEqualTo("tenant-meta");
        assertThat(handler.observedContext.principal().actorId()).isEqualTo("actor-meta");
    }

    @Test
    void consume_shouldSkipDuplicateEvents() throws Exception {
        KafkaEnvelope<Map<String, String>> envelope =
                KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));
        dedup.markProcessed(envelope.eventId(), 3600);
        String json = mapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", json);
        addContextHeaders(record);

        service.consume(record);

        assertThat(handler.envelope).isNull();
        assertThat(dedup.marked).containsExactly(envelope.eventId());
    }

    @Test
    void consume_shouldNotDispatchOnMalformedJson() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", "not-json");
        addContextHeaders(record);

        service.consume(record);

        assertThat(handler.envelope).isNull();
        assertThat(dedup.marked).isEmpty();
        assertThat(ContextAccessor.get()).isNull();
    }

    @Test
    void consume_shouldNotRebuildContextWhenCorrelationHeadersMissing() throws Exception {
        KafkaEnvelope<Map<String, String>> envelope =
                KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));
        String json = mapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 5L, "key-1", json);
        record.headers().add("x-request-id", "req-1".getBytes(StandardCharsets.UTF_8));

        service.consume(record);

        assertThat(handler.envelope).isNotNull();
        assertThat(handler.observedContext).isNull();
        assertThat(dedup.marked).hasSize(1);
    }

    private static void addContextHeaders(ConsumerRecord<String, String> record) {
        record.headers().add("x-request-id", "req-1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-correlation-id", "corr-1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-tenant-id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-actor-id", "actor-9".getBytes(StandardCharsets.UTF_8));
    }

    public static class RecordingHandler {
        public KafkaEnvelope<?> envelope;
        public Map<String, String> headers;
        public com.omnixys.context.ContextSnapshot observedContext;

        public void handle(KafkaEnvelope<?> envelope, Map<String, String> headers) {
            this.envelope = envelope;
            this.headers = headers;
            this.observedContext = ContextAccessor.get();
        }
    }

    static class RecordingDedup implements EventDeduplicationService {
        final Set<String> processed = new HashSet<>();
        final List<String> marked = new ArrayList<>();

        @Override
        public boolean isDuplicate(String eventId) {
            return processed.contains(eventId);
        }

        @Override
        public void markProcessed(String eventId, long ttlSeconds) {
            if (processed.add(eventId)) {
                marked.add(eventId);
            }
        }
    }
}
