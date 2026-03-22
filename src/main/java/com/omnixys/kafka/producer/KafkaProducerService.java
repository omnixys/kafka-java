package com.omnixys.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.kafka.model.KafkaHeaderMapper;
import com.omnixys.kafka.utils.KafkaHeaderSetter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import com.omnixys.observability.context.TraceContextExtractor;
import com.omnixys.observability.propagation.ContextPropagator;
import com.omnixys.observability.propagation.W3CTraceContextPropagator;
import com.omnixys.kafka.utils.KafkaHeaderCarrier;

/**
 * Kafka producer with OpenTelemetry propagation.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public <T> void send(String topic, KafkaEnvelope<T> envelope) {
        send(topic, envelope, null);
    }


    public <T> void send(
            String topic,
            KafkaEnvelope<T> envelope,
            String key
    ) {
        try {
            Context context = Context.current();
            String json = objectMapper.writeValueAsString(envelope);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topic, key, json);

            GlobalOpenTelemetry.get()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(context, record.headers(), new KafkaHeaderSetter());

            log.debug("Envelope meta Data = {}", envelope.metadata());

            KafkaHeaderMapper.addMetadata(
                    record.headers(),
                    envelope.metadata()
            );

            record.headers().forEach(h -> {
                log.debug("HEADER {} = {}", h.key(), new String(h.value()));
            });

            kafkaTemplate.send(record);

            log.debug("Kafka message sent topic={} key={}", topic, key);

        } catch (Exception e) {
            throw new RuntimeException("Kafka send failed topic=" + topic, e);
        }
    }
}