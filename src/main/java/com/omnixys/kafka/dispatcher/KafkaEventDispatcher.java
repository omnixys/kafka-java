package com.omnixys.kafka.dispatcher;

import com.omnixys.kafka.model.KafkaEnvelope;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class KafkaEventDispatcher {

    private final Map<String, Handler> handlers = new HashMap<>();

    public void register(String topic, Object bean, Method method) {
        handlers.put(topic, new Handler(bean, method));

        log.debug("Registered Kafka handler topic={} method={}#{}",
                topic,
                bean.getClass().getSimpleName(),
                method.getName()
        );
    }

    public void dispatch(String topic, KafkaEnvelope<?> envelope, Map<String, String> headers) {
        Handler handler = handlers.get(topic);

        if (handler == null) {
            log.warn("No Kafka handler registered for topic={}", topic);
            return;
        }

        try {
            int paramCount = handler.method.getParameterCount();

            if (paramCount == 1) {
                handler.method.invoke(handler.bean, envelope);
            } else {
                handler.method.invoke(handler.bean, envelope, headers);
            }

        } catch (Exception e) {
            throw new RuntimeException("Kafka dispatch failed for topic=" + topic, e);
        }
    }

    /**
     * 🔥 CRITICAL: used by Spring KafkaListener via SpEL
     */
    public String[] getTopics() {
        Set<String> topics = handlers.keySet();

        log.debug("Kafka subscribing to topics={}", topics);

        return topics.toArray(new String[0]);
    }

    private record Handler(Object bean, Method method) {}
}