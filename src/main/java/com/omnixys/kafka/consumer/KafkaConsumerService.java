package com.omnixys.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnixys.kafka.config.OmnixysKafkaProperties;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.kafka.utils.KafkaHeaderCarrier;
import com.omnixys.kafka.utils.KafkaHeaderGetter;
import com.omnixys.kafka.utils.KafkaHeaderUtils;
import com.omnixys.observability.context.ITraceContext;
import com.omnixys.observability.propagation.ContextPropagator;
import com.omnixys.observability.propagation.W3CTraceContextPropagator;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka consumer with OpenTelemetry tracing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final KafkaEventDispatcher dispatcher;
    private final ObjectMapper mapper;
    private final OmnixysKafkaProperties properties;

    @KafkaListener(
            topics = "#{@kafkaEventDispatcher.getTopics()}",
            groupId = "${omnixys.kafka.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {

        log.debug("Kafka received topic={} key={}",
                record.topic(),
                record.key()
        );

        record.headers().forEach(h -> {
            log.debug("CONSUMER HEADER {} = {}", h.key(), new String(h.value()));
        });

        Context parentContext = GlobalOpenTelemetry.get()
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), new KafkaHeaderGetter());

        Tracer tracer = GlobalOpenTelemetry.getTracer("omnixys.kafka.consumer");

        var span = tracer.spanBuilder("kafka.consume." + record.topic())
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(parentContext)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination.name", record.topic())
                .setAttribute("messaging.kafka.partition", record.partition())
                .setAttribute("messaging.kafka.offset", record.offset())
                .setAttribute("messaging.consumer.group", properties.getGroupId())
                .setAttribute("messaging.kafka.message.key", record.key() != null ? record.key() : "")
                .setAttribute("messaging.method", "receive")
                .startSpan();

        log.debug("NEW spanId={}", span.getSpanContext().getSpanId());
        log.debug("PARENT traceId={}", span.getSpanContext().getTraceId());

        try (var scope = span.makeCurrent()) {

            KafkaEnvelope<?> envelope = mapper.readValue(record.value(), new TypeReference<KafkaEnvelope<Object>>() {});

            if (envelope.metadata() != null) {
                envelope.metadata().forEach((key, value) -> {
                    if (key != null && value != null) {
                        span.setAttribute("app." + key, value);
                    }
                });
            }

            Map<String, String> headerMap =
                    KafkaHeaderUtils.toMap(record.headers());

            log.debug("Headers: {}", headerMap);

            dispatcher.dispatch(record.topic(), envelope, headerMap);

        } catch (Exception e) {

            log.error("Kafka consume failed topic={} partition={} offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e
            );

            span.recordException(e);
            span.setStatus(StatusCode.ERROR);

        } finally {
            span.end();
        }
    }
}