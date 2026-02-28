package com.wx.bus.infrastructure.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 消费者队列与绑定：当配置了 bus.consumer.consumer-id 与 bus.consumer.topics 时，声明专用队列并绑定到业务事件交换机。
 * <p>队列名为 bus.consumer.{consumerId}，routing key 为各 topic。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "bus.consumer", name = "consumer-id")
public class ConsumerBindingConfig {

    private static final String QUEUE_PREFIX = "bus.consumer.";

    private final TopicExchange eventsExchange;
    private final String topicsCsv;

    public ConsumerBindingConfig(
        @Qualifier("eventsExchange") TopicExchange eventsExchange,
        @Value("${bus.consumer.topics:}") String topicsCsv
    ) {
        this.eventsExchange = eventsExchange;
        this.topicsCsv = topicsCsv;
    }

    @Bean("busConsumerQueue")
    Queue busConsumerQueue(
        @Value("${bus.consumer.consumer-id}") String consumerId
    ) {
        return new Queue(QUEUE_PREFIX + consumerId, true, false, false);
    }

    /**
     * 声明消费者 bindings（以及队列本身）为 Declarables，让 Spring AMQP 在启动时自动声明到 broker。
     * <p>避免在 @Configuration 内部通过字段注入同配置类的 @Bean 产生循环依赖。</p>
     */
    @Bean
    Declarables busConsumerDeclarables(@Qualifier("busConsumerQueue") Queue busConsumerQueue) {
        List<Declarable> declarables = new ArrayList<>();
        declarables.add(busConsumerQueue);
        List<String> topics = Stream.of(topicsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        for (String topic : topics) {
            Binding binding = BindingBuilder.bind(busConsumerQueue).to(eventsExchange).with(topic);
            declarables.add(binding);
        }
        return new Declarables(declarables);
    }
}
