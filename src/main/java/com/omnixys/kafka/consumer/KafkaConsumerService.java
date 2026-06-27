package com.omnixys.kafka.consumer;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.omnixys.context.ClientMetadata;
import com.omnixys.context.ContextAccessor;
import com.omnixys.context.ContextSnapshot;
import com.omnixys.context.PrincipalContext;
import com.omnixys.context.TenantContext;
import com.omnixys.context.TraceMetadata;
import com.omnixys.context.TransportMetadata;
import com.omnixys.kafka.config.OmnixysKafkaProperties;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.model.KafkaEnvelope;
import com.omnixys.kafka.utils.KafkaHeaderGetter;
import com.omnixys.kafka.utils.KafkaHeaderUtils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.Map;

@Slf4j
public class KafkaConsumerService {

    private static final long DEDUP_TTL_SECONDS = 86400;

    private final KafkaEventDispatcher dispatcher;
    private final ObjectMapper mapper;
    private final OmnixysKafkaProperties properties;
    private final OpenTelemetry openTelemetry;
    private final EventDeduplicationService dedup;

    public KafkaConsumerService(
            KafkaEventDispatcher dispatcher,
            ObjectMapper mapper,
            OmnixysKafkaProperties properties,
            OpenTelemetry openTelemetry,
            EventDeduplicationService dedup
    ) {
        this.dispatcher = dispatcher;
        this.mapper = mapper;
        this.properties = properties;
        this.openTelemetry = openTelemetry;
        this.dedup = dedup;
    }

    @KafkaListener(
            topics = "#{@kafkaEventDispatcher.getTopics()}",
            groupId = "${omnixys.kafka.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {

        log.debug("Kafka received topic={} key={}",
                record.topic(),
                record.key()
        );

        Context parentContext = openTelemetry
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), new KafkaHeaderGetter());

        Tracer tracer = openTelemetry.getTracer("omnixys.kafka.consumer");

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

        try (var scope = span.makeCurrent()) {

            KafkaEnvelope<?> envelope =
                    mapper.readValue(
                            record.value(),
                            new TypeReference<KafkaEnvelope<Object>>() {}
                    );

            if (envelope.eventId() != null && dedup.isDuplicate(envelope.eventId())) {
                log.warn("Duplicate event detected topic={} eventId={}", record.topic(), envelope.eventId());
                span.setAttribute("app.dedup", "skipped");
                return;
            }

            Map<String, String> headerMap = KafkaHeaderUtils.toMap(record.headers());

            if (headerMap.containsKey("x-meta-tenantId")) {
                span.setAttribute("app.x-meta-tenantId", headerMap.get("x-meta-tenantId"));
            }
            if (headerMap.containsKey("x-meta-actorId")) {
                span.setAttribute("app.x-meta-actorId", headerMap.get("x-meta-actorId"));
            }

            rebuildContext(record, headerMap);

            dispatcher.dispatch(record.topic(), envelope, headerMap);

            if (envelope.eventId() != null) {
                dedup.markProcessed(envelope.eventId(), DEDUP_TTL_SECONDS);
            }
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
            ContextAccessor.clear();
            span.end();
        }
    }

    private void rebuildContext(ConsumerRecord<String, String> record, Map<String, String> headers) {
        String requestId = headers.get("x-request-id");
        String correlationId = headers.get("x-correlation-id");
        if (requestId == null || correlationId == null) return;

        String tenantId = headers.get("x-tenant-id");
        if (tenantId == null) tenantId = headers.get("x-meta-tenantId");
        String actorId = headers.get("x-actor-id");
        if (actorId == null) actorId = headers.get("x-meta-actorId");

        TenantContext tenant = tenantId != null ? new TenantContext(tenantId, "kafka", false) : null;
        PrincipalContext principal = actorId != null
                ? new PrincipalContext(actorId, actorId, actorId, tenantId != null ? tenantId : "unknown", List.of(), null, null, null)
                : null;
        ClientMetadata client = new ClientMetadata(null, null, null, null, null, null, null, null, null);
        TransportMetadata transport = new TransportMetadata(
                "kafka", null, null, null, null, null,
                record.topic(),
                record.partition(),
                String.valueOf(record.offset()),
                properties.getGroupId(),
                null, null, null
        );

        ContextSnapshot snapshot = new ContextSnapshot(
                requestId, correlationId, System.currentTimeMillis(),
                tenant, principal, client, transport, new TraceMetadata(null, null)
        );
        ContextAccessor.set(snapshot);
    }
}
