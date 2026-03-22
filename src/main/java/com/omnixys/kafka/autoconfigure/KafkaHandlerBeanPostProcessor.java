package com.omnixys.kafka.autoconfigure;

import com.omnixys.kafka.annotation.KafkaEvent;
import com.omnixys.kafka.dispatcher.KafkaEventDispatcher;
import com.omnixys.kafka.model.KafkaEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
public class KafkaHandlerBeanPostProcessor
        implements BeanPostProcessor, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {

        Class<?> targetClass = bean.getClass();

        for (Method method : targetClass.getDeclaredMethods()) {
            KafkaEvent annotation = method.getAnnotation(KafkaEvent.class);

            if (annotation == null) continue;

            validateMethod(method);

            KafkaEventDispatcher dispatcher =
                    beanFactory.getBean(KafkaEventDispatcher.class);

            dispatcher.register(annotation.topic(), bean, method);

            log.debug("Registered Kafka handler topic={} method={}",
                    annotation.topic(),
                    method
            );
        }

        return bean;
    }

    private void validateMethod(Method method) {

        int paramCount = method.getParameterCount();

        if (paramCount == 1) {
            // ✔️ handle(KafkaEnvelope)
            if (!KafkaEnvelope.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalStateException(
                        "@KafkaEvent method must have parameter of type KafkaEnvelope: " + method
                );
            }
            return;
        }

        if (paramCount == 2) {
            // ✔️ handle(KafkaEnvelope, Map<String,String>)
            Class<?>[] types = method.getParameterTypes();

            if (!KafkaEnvelope.class.isAssignableFrom(types[0])) {
                throw new IllegalStateException(
                        "First parameter must be KafkaEnvelope: " + method
                );
            }

            if (!Map.class.isAssignableFrom(types[1])) {
                throw new IllegalStateException(
                        "Second parameter must be Map<String,String>: " + method
                );
            }

            return;
        }

        throw new IllegalStateException(
                "@KafkaEvent method must have 1 or 2 parameters: " + method
        );
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory)
            throws BeansException {
        this.beanFactory = beanFactory;
    }
}