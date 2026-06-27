package com.omnixys.kafka.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.kafka.config.OmnixysKafkaProperties;
import com.omnixys.kafka.consumer.EventDeduplicationService;
import com.omnixys.kafka.consumer.KafkaConsumerService;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.producer.KafkaProducerService;
import com.omnixys.observability.api.TracePropagation;
import io.opentelemetry.api.OpenTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import com.omnixys.kafka.autoconfigure.KafkaHandlerBeanPostProcessor;
import com.omnixys.kafka.health.KafkaHealthIndicator;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableConfigurationProperties(OmnixysKafkaProperties.class)
@ConditionalOnProperty(
        prefix = "omnixys.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
public class KafkaAutoConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public KafkaEventDispatcher kafkaEventDispatcher() {
        return new KafkaEventDispatcher();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public KafkaHandlerBeanPostProcessor kafkaHandlerBeanPostProcessor() {
        return new KafkaHandlerBeanPostProcessor();
    }


    @Bean
    @ConditionalOnMissingBean
    public KafkaProducerService kafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            TracePropagation<Object> tracing
    ) {
        return new KafkaProducerService(kafkaTemplate, objectMapper, tracing);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventDeduplicationService eventDeduplicationService() {
        return new EventDeduplicationService() {
            private final java.util.Set<String> processed = java.util.concurrent.ConcurrentHashMap.newKeySet();

            @Override
            public boolean isDuplicate(String eventId) {
                return processed.contains(eventId);
            }

            @Override
            public void markProcessed(String eventId, long ttlSeconds) {
                processed.add(eventId);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerService kafkaConsumerService(
            KafkaEventDispatcher dispatcher,
            ObjectMapper mapper,
            OmnixysKafkaProperties properties,
            OpenTelemetry openTelemetry,
            EventDeduplicationService dedup
    ) {
        return new KafkaConsumerService(dispatcher, mapper, properties, openTelemetry, dedup);
    }


    @Bean(name = "kafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            OmnixysKafkaProperties props,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();


        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(props.getConsumer().getConcurrency());

        CommonErrorHandler errorHandler = createErrorHandler(props, kafkaTemplate);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private CommonErrorHandler createErrorHandler(OmnixysKafkaProperties props, KafkaTemplate<String, String> kafkaTemplate) {
        var backOff = new ExponentialBackOff();
        backOff.setInitialInterval(500L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30000L);
        backOff.setMaxElapsedTime(props.getDlq().getMaxRetries() * 10000L);

        if (props.getDlq().isEnabled()) {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                    kafkaTemplate,
                    (record, ex) -> new org.apache.kafka.common.TopicPartition(
                            record.topic() + props.getDlq().getSuffix(),
                            record.partition()
                    )
            );
            return new DefaultErrorHandler(recoverer, backOff);
        }
        return new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Kafka failed permanently topic={} partition={} offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception
                ),
                backOff
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, String> consumerFactory(OmnixysKafkaProperties props) {

        // 🔥 FAIL FAST VALIDATION
        if (props.getBootstrapServers() == null) {
            throw new IllegalStateException("omnixys.kafka.bootstrap-servers must be set");
        }
        if (props.getGroupId() == null) {
            throw new IllegalStateException("omnixys.kafka.group-id must be set");
        }

        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, props.getGroupId());

        if (props.getClientId() != null) {
            config.put(ConsumerConfig.CLIENT_ID_CONFIG, props.getClientId());
        }

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, props.getConsumer().isAutoCommit());

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, String> producerFactory(OmnixysKafkaProperties props) {

        if (props.getBootstrapServers() == null) {
            throw new IllegalStateException("omnixys.kafka.bootstrap-servers must be set");
        }

        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());

        if (props.getClientId() != null) {
            config.put(ProducerConfig.CLIENT_ID_CONFIG, props.getClientId());
        }

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        config.put(ProducerConfig.ACKS_CONFIG, props.getProducer().getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, props.getProducer().getRetries());
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    public KafkaHealthIndicator kafkaHealthIndicator(OmnixysKafkaProperties props) {
        return new KafkaHealthIndicator(props.getBootstrapServers());
    }
}