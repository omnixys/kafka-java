package com.omnixys.kafka.producer;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.context.ContextAccessor;
import com.omnixys.context.ContextSnapshot;
import com.omnixys.kafka.adapter.KafkaHeaderAdapter;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.kafka.model.KafkaHeaderMapper;
import com.omnixys.kafka.model.KafkaMetaData;
import com.omnixys.observability.api.TraceContext;
import com.omnixys.observability.api.TracePropagation;
import com.omnixys.observability.api.TraceSpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;

import static com.omnixys.kafka.utils.MetadataKeys.SPAN_ID;
import static com.omnixys.kafka.utils.MetadataKeys.TRACE_ID;

/**
 * Kafka producer with OpenTelemetry propagation.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TracePropagation<Object> tracing;

    public <T> void send(
            String topic,
            KafkaEnvelope<T> envelope,
            KafkaMetaData meta
    ) {
        send(topic, envelope, meta, null);
    }

    public <T> void send(
            String topic,
            KafkaEnvelope<T> envelope,
            KafkaMetaData meta,
            TraceContext ctx,
            String key
    ) {
        try {
        String json = objectMapper.writeValueAsString(envelope);

        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, key, json);

        tracing.inject(new KafkaHeaderAdapter(record.headers()));

        KafkaHeaderMapper.addMetadata(
                record.headers(),
                meta
        );

        if (ctx != null) {
            record.headers().add(
                    TRACE_ID,
                    ctx.traceId().getBytes(StandardCharsets.UTF_8)
            );

            record.headers().add(
                    SPAN_ID,
                    ctx.spanId().getBytes(StandardCharsets.UTF_8)
            );
        }
        injectContextHeaders(record);

        sendAsync(record, topic);
        } catch (Exception e) {
            throw new RuntimeException("Kafka send failed topic=" + topic, e);
        }
    }


    public <T> void send(
            String topic,
            KafkaEnvelope<T> envelope,
            KafkaMetaData meta,
            String key
    ) {
        try {
            tracing.runWithSpan("Kafka PRODUCE " + topic, TraceSpanKind.PRODUCER, () -> {
                try {
                    String json = objectMapper.writeValueAsString(envelope);

                    ProducerRecord<String, String> record =
                            new ProducerRecord<>(topic, key, json);

                    tracing.inject(new KafkaHeaderAdapter(record.headers()));

                    KafkaHeaderMapper.addMetadata(
                            record.headers(),
                            meta
                    );

                    TraceContext ctx = tracing.currentContext();
                    if (ctx != null) {
                        record.headers().add(
                                TRACE_ID,
                                ctx.traceId().getBytes(StandardCharsets.UTF_8)
                        );

                        record.headers().add(
                                SPAN_ID,
                                ctx.spanId().getBytes(StandardCharsets.UTF_8)
                        );
                    }
                    injectContextHeaders(record);

                    sendAsync(record, topic);
                    log.debug("Kafka message sent topic={} key={}", topic, key);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException("Kafka send failed topic=" + topic, e);
                } finally {

                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Kafka send failed topic=" + topic, e);
        }
    }

    private void sendAsync(ProducerRecord<String, String> record, String topic) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka send failed topic={} key={}", topic, record.key(), ex);
            } else {
                log.debug("Kafka message sent topic={} key={} offset={}", topic, record.key(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    private void injectContextHeaders(ProducerRecord<String, String> record) {
        ContextSnapshot snapshot = ContextAccessor.get();
        if (snapshot == null) return;
        record.headers().add("x-request-id", snapshot.requestId().getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-correlation-id", snapshot.correlationId().getBytes(StandardCharsets.UTF_8));
        if (snapshot.tenant() != null) {
            byte[] tenantBytes = snapshot.tenant().tenantId().getBytes(StandardCharsets.UTF_8);
            record.headers().add("x-tenant-id", tenantBytes);
            record.headers().add("x-meta-tenantId", tenantBytes);
        }
        if (snapshot.principal() != null) {
            String actorId = snapshot.principal().actorId() != null
                    ? snapshot.principal().actorId()
                    : snapshot.principal().subject();
            byte[] actorBytes = actorId.getBytes(StandardCharsets.UTF_8);
            record.headers().add("x-actor-id", actorBytes);
            record.headers().add("x-meta-actorId", actorBytes);
        }
    }
}