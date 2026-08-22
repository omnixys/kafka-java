package com.omnixys.kafka.producer;

import com.omnixys.context.ClientMetadata;
import com.omnixys.context.ContextAccessor;
import com.omnixys.context.ContextSnapshot;
import com.omnixys.context.PrincipalContext;
import com.omnixys.context.TenantContext;
import com.omnixys.context.TransportMetadata;
import com.omnixys.kafka.model.EventType;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.kafka.model.KafkaMetaData;
import com.omnixys.observability.api.HeaderSetter;
import com.omnixys.observability.api.TraceContext;
import com.omnixys.observability.api.TraceContextSnapshot;
import com.omnixys.observability.api.TracePropagation;
import com.omnixys.observability.api.TraceSpanKind;
import com.omnixys.observability.api.TraceSupplier;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerServiceTest {

    private final CapturingKafkaTemplate template = new CapturingKafkaTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final FakeTracePropagation tracing = new FakeTracePropagation();
    private final KafkaProducerService service = new KafkaProducerService(template, mapper, tracing);

    @AfterEach
    void tearDown() {
        ContextAccessor.clear();
    }

    @Test
    void send_shouldSerializeEnvelopeAndSendWithKey() {
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.EVENT);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, "key-1");

        assertThat(template.captured).isNotNull();
        assertThat(template.captured.topic()).isEqualTo("orders");
        assertThat(template.captured.key()).isEqualTo("key-1");
        assertThat(template.captured.value()).isEqualTo(json(envelope));
    }

    @Test
    void send_shouldAddMetadataHeaders() {
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.COMMAND);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, null);

        assertThat(header(template.captured, "x-meta-service")).isEqualTo("svc");
        assertThat(header(template.captured, "x-meta-version")).isEqualTo("1.0");
        assertThat(header(template.captured, "x-meta-class")).isEqualTo("OrderHandler");
        assertThat(header(template.captured, "x-meta-operation")).isEqualTo("create");
        assertThat(header(template.captured, "x-meta-type")).isEqualTo("COMMAND");
    }

    @Test
    void send_shouldInjectTraceHeadersViaPropagation() {
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.EVENT);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, "key-1");

        assertThat(tracing.injectCount).isEqualTo(1);
        assertThat(header(template.captured, "traceparent")).isEqualTo("00-abcdef");
    }

    @Test
    void send_shouldAddTraceContextHeadersWhenContextProvided() {
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.EVENT);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, new TraceContext("trace-id", "span-id"), null);

        assertThat(header(template.captured, "x-meta-traceId")).isEqualTo("trace-id");
        assertThat(header(template.captured, "x-meta-spanId")).isEqualTo("span-id");
    }

    @Test
    void send_shouldInjectContextHeadersFromContextAccessor() {
        ContextAccessor.set(snapshot());
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.EVENT);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, "key-1");

        assertThat(header(template.captured, "x-request-id")).isEqualTo("req-1");
        assertThat(header(template.captured, "x-correlation-id")).isEqualTo("corr-1");
        assertThat(header(template.captured, "x-tenant-id")).isEqualTo("tenant-a");
        assertThat(header(template.captured, "x-meta-tenantId")).isEqualTo("tenant-a");
        assertThat(header(template.captured, "x-actor-id")).isEqualTo("actor-9");
        assertThat(header(template.captured, "x-meta-actorId")).isEqualTo("actor-9");
    }

    @Test
    void send_shouldSkipContextHeadersWhenNoContextPresent() {
        KafkaMetaData meta = new KafkaMetaData("svc", "1.0", "OrderHandler", "create", EventType.EVENT);
        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");

        service.send("orders", envelope, meta, "key-1");

        assertThat(header(template.captured, "x-request-id")).isNull();
        assertThat(header(template.captured, "x-tenant-id")).isNull();
    }

    private String json(KafkaEnvelope<String> envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String header(ProducerRecord<?, ?> record, String key) {
        var header = record.headers().lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    private static ContextSnapshot snapshot() {
        return new ContextSnapshot(
                "req-1", "corr-1", 1_700_000_000_000L,
                new TenantContext("tenant-a", "trusted-header", true),
                new PrincipalContext("usr-1", "actor-9", "usr-1", "tenant-a",
                        List.of("admin"), "sess-1", "mfa", 1_700_000_000_000L),
                new ClientMetadata(null, null, null, null, null, null, null, null, null),
                new TransportMetadata("http", "GET", "/x", null, "HTTP/1.1", "host",
                        null, null, null, null, null, null, null),
                null
        );
    }

    static class CapturingKafkaTemplate extends KafkaTemplate<String, String> {
        ProducerRecord<String, String> captured;

        CapturingKafkaTemplate() {
            super(new DefaultKafkaProducerFactory<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            )));
        }

        @Override
        public CompletableFuture<SendResult<String, String>> send(ProducerRecord<String, String> record) {
            this.captured = record;
            return CompletableFuture.failedFuture(new RuntimeException("no-op"));
        }
    }

    static class FakeTracePropagation implements TracePropagation<Object> {
        int injectCount = 0;

        @Override
        public void inject(Object carrier) {
            injectCount++;
            ((HeaderSetter) carrier).set("traceparent", "00-abcdef");
        }

        @Override
        public TraceContext currentContext() {
            return new TraceContext("current-trace", "current-span");
        }

        @Override
        public TraceContextSnapshot capture() {
            return null;
        }

        @Override
        public <T> T runWithSpan(String name, TraceSpanKind kind, TraceSupplier<T> fn) {
            try {
                return fn.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
