package com.omnixys.kafka.dispatcher;

import com.omnixys.kafka.model.KafkaEnvelope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaEventDispatcherTest {

    private final KafkaEventDispatcher dispatcher = new KafkaEventDispatcher();

    static class SingleParamHandler {
        final List<KafkaEnvelope<?>> received = new ArrayList<>();

        public void handle(KafkaEnvelope<?> envelope) {
            received.add(envelope);
        }
    }

    static class TwoParamHandler {
        final List<Map<String, String>> received = new ArrayList<>();

        public void handle(KafkaEnvelope<?> envelope, Map<String, String> headers) {
            received.add(headers);
        }
    }

    static class ThrowingHandler {
        public void handle(KafkaEnvelope<?> envelope) {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void registerAndDispatch_shouldInvokeSingleParamHandler() throws Exception {
        var handler = new SingleParamHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class));

        KafkaEnvelope<String> envelope = KafkaEnvelope.of("order.created", "svc", "1.0", "payload");
        dispatcher.dispatch("orders", envelope, Map.of());

        assertThat(handler.received).containsExactly(envelope);
    }

    @Test
    void dispatch_shouldPassHeadersToTwoParamHandler() throws Exception {
        var handler = new TwoParamHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class, Map.class));

        Map<String, String> headers = Map.of("x-tenant-id", "tenant-a");
        dispatcher.dispatch("orders", KafkaEnvelope.of("order.created", "svc", "1.0", "p"), headers);

        assertThat(handler.received).containsExactly(headers);
    }

    @Test
    void dispatch_shouldIgnoreUnknownTopics() throws Exception {
        var handler = new SingleParamHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class));

        dispatcher.dispatch("unknown.topic", KafkaEnvelope.of("order.created", "svc", "1.0", "p"), Map.of());

        assertThat(handler.received).isEmpty();
    }

    @Test
    void dispatch_shouldWrapInvocationExceptions() throws Exception {
        var handler = new ThrowingHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class));

        assertThatThrownBy(() -> dispatcher.dispatch("orders", KafkaEnvelope.of("order.created", "svc", "1.0", "p"), Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("orders")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void getTopics_shouldReturnRegisteredTopics() throws Exception {
        var handler = new SingleParamHandler();
        dispatcher.register("orders", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class));
        dispatcher.register("users", handler,
                handler.getClass().getMethod("handle", KafkaEnvelope.class));

        assertThat(dispatcher.getTopics()).containsExactlyInAnyOrder("orders", "users");
    }

    @Test
    void register_shouldReplaceExistingTopicHandler() throws Exception {
        var first = new SingleParamHandler();
        var second = new SingleParamHandler();
        dispatcher.register("orders", first,
                first.getClass().getMethod("handle", KafkaEnvelope.class));
        dispatcher.register("orders", second,
                second.getClass().getMethod("handle", KafkaEnvelope.class));

        dispatcher.dispatch("orders", KafkaEnvelope.of("order.created", "svc", "1.0", "p"), Map.of());

        assertThat(first.received).isEmpty();
        assertThat(second.received).hasSize(1);
    }
}
