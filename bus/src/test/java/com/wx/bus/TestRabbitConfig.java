package com.wx.bus;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 测试用 RabbitMQ：声明测试队列并绑定到业务事件交换机，用于验证事件是否发到 MQ。
 * <p>测试队列使用持久化、非 autoDelete；测试类在 @BeforeEach 中显式 declare 确保 broker 上存在。</p>
 */
@Configuration
public class TestRabbitConfig {

    public static final String TEST_QUEUE_ORDER_PURCHASED = "bus-test-order.purchased";

    @Bean
    Queue testOrderPurchasedQueue() {
        return new Queue(TEST_QUEUE_ORDER_PURCHASED, true, false, false);
    }

    @Bean
    Binding testOrderPurchasedBinding(
        Queue testOrderPurchasedQueue,
        @Qualifier("eventsExchange") TopicExchange eventsExchange
    ) {
        return BindingBuilder.bind(testOrderPurchasedQueue).to(eventsExchange).with("order.purchased");
    }

    public static final String TEST_QUEUE_MEMBER_LOTTERY = "bus-test-member.lottery.added";

    @Bean
    Queue testMemberLotteryQueue() {
        return new Queue(TEST_QUEUE_MEMBER_LOTTERY, true, false, false);
    }

    @Bean
    Binding testMemberLotteryBinding(
        Queue testMemberLotteryQueue,
        @Qualifier("eventsExchange") TopicExchange eventsExchange
    ) {
        return BindingBuilder.bind(testMemberLotteryQueue).to(eventsExchange).with("member.lottery.added");
    }

    public static final String TEST_QUEUE_MESSAGE_MULTI = "bus-test-message.multi.send";

    @Bean
    Queue testMessageMultiQueue() {
        return new Queue(TEST_QUEUE_MESSAGE_MULTI, true, false, false);
    }

    @Bean
    Binding testMessageMultiBinding(
        Queue testMessageMultiQueue,
        @Qualifier("eventsExchange") TopicExchange eventsExchange
    ) {
        return BindingBuilder.bind(testMessageMultiQueue).to(eventsExchange).with("message.multi.send");
    }
}
