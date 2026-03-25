package com.omnixys.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        record.headers().forEach(h -> {
            log.info("HEADER {} = {}", h.key(), new String(h.value()));
        });

        kafkaTemplate.send(record);
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

                    record.headers().forEach(h -> {
                        log.info("HEADER {} = {}", h.key(), new String(h.value()));
                    });


                    kafkaTemplate.send(record);
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
}