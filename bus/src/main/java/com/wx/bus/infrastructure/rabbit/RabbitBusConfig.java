package com.wx.bus.infrastructure.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 声明：业务事件 topic 交换机、消费汇总专用队列。
 * <ul>
 *   <li>eventsExchange：业务事件发布到此，routing key = topic，各消费者自行绑定队列</li>
 *   <li>rollupQueue：消费汇总专用，单消费者串行消费 eventId 并回写 events.status</li>
 * </ul>
 */
@Configuration
@EnableRabbit
public class RabbitBusConfig {

    @Value("${bus.mq.events-exchange:bus.events}")
    private String eventsExchangeName;

    @Value("${bus.mq.rollup-queue:bus.consumption-rollup}")
    private String rollupQueueName;

    /** 业务事件 topic 交换机，持久化、非自动删除 */
    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(eventsExchangeName, true, false);
    }

    /** 消费汇总专用队列，持久化，由 {@link RollupListener} 消费 */
    @Bean
    Queue rollupQueue() {
        return new Queue(rollupQueueName, true, false, false);
    }
}
