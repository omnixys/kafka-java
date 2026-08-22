package com.omnixys.kafka.autoconfigure;

import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.model.KafkaEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaHandlerBeanPostProcessorTest {

    private final KafkaEventDispatcher dispatcher = new KafkaEventDispatcher();

    static class ValidHandler {
        @KafkaEvent(topic = "orders.created")
        public void onOrderCreated(KafkaEnvelope<?> envelope) {
        }

        @KafkaEvent(topic = "users.updated")
        public void onUserUpdated(KafkaEnvelope<?> envelope, Map<String, String> headers) {
        }
    }

    static class InvalidArity {
        @KafkaEvent(topic = "bad")
        public void handle(String notAnEnvelope) {
        }
    }

    static class InvalidSecondParam {
        @KafkaEvent(topic = "bad")
        public void handle(KafkaEnvelope<?> envelope, Integer notAMap) {
        }
    }

    static class InvalidCount {
        @KafkaEvent(topic = "bad")
        public void handle() {
        }
    }

    static class InvalidCountThree {
        @KafkaEvent(topic = "bad")
        public void handle(KafkaEnvelope<?> a, Map<String, String> b, String c) {
        }
    }

    private KafkaHandlerBeanPostProcessor processor() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("kafkaEventDispatcher", dispatcher);
        KafkaHandlerBeanPostProcessor processor = new KafkaHandlerBeanPostProcessor();
        processor.setBeanFactory(beanFactory);
        return processor;
    }

    @Test
    void shouldRegisterAnnotatedMethodsWithDispatcher() {
        processor().postProcessAfterInitialization(new ValidHandler(), "validHandler");

        assertThat(dispatcher.getTopics()).containsExactlyInAnyOrder("orders.created", "users.updated");
    }

    @Test
    void shouldNotRegisterBeansWithoutAnnotations() {
        Object input = new Object();
        Object bean = processor().postProcessAfterInitialization(input, "plain");

        assertThat(bean).isSameAs(input);
        assertThat(dispatcher.getTopics()).isEmpty();
    }

    @Test
    void shouldRejectSingleParamMethodNotTakingKafkaEnvelope() {
        assertThatThrownBy(() -> processor().postProcessAfterInitialization(new InvalidArity(), "invalid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KafkaEnvelope");
    }

    @Test
    void shouldRejectTwoParamMethodWithNonMapSecondParam() {
        assertThatThrownBy(() -> processor().postProcessAfterInitialization(new InvalidSecondParam(), "invalid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Map");
    }

    @Test
    void shouldRejectZeroParamMethod() {
        assertThatThrownBy(() -> processor().postProcessAfterInitialization(new InvalidCount(), "invalid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 or 2 parameters");
    }

    @Test
    void shouldRejectThreeParamMethod() {
        assertThatThrownBy(() -> processor().postProcessAfterInitialization(new InvalidCountThree(), "invalid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 or 2 parameters");
    }
}
