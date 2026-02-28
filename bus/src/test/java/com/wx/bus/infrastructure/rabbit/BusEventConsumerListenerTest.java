package com.wx.bus.infrastructure.rabbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wx.bus.BusTestApplication;
import com.wx.bus.application.ConsumptionRollupService;
import com.wx.bus.application.EventPublishService;
import com.wx.bus.application.port.BusEventConsumer;
import com.wx.bus.application.port.ConsumptionFeedbackPort;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.EventStatus;
import com.wx.bus.domain.Initiator;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.bus.infrastructure.mongo.TopicConsumerDocument;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/**
 * BusEventConsumerListener 集成测试（不使用 mock）：
 * <p>使用本机 MongoDB + RabbitMQ（见 src/test/resources/application.yml），验证：</p>
 * <ul>
 *   <li>发布事件后消息进入消费者队列并被消费</li>
 *   <li>消费成功/失败会写入 event_consumptions，并触发 rollup 回写 events.status</li>
 * </ul>
 */
@SpringBootTest(
    classes = {BusTestApplication.class, BusEventConsumerListenerTest.TestConsumerConfig.class},
    properties = {
        "bus.consumer.consumer-id=member-service",
        "bus.consumer.topics=order.purchased",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
    }
)
@ActiveProfiles("test")
class BusEventConsumerListenerTest {

    private static final String CONSUMER_ID = "member-service";
    private static final String EVENT_ID = "evt-test-001";
    private static final String TOPIC = "order.purchased";
    private static final String FAILURE_MESSAGE = "业务处理失败";

    @Autowired
    private EventPublishService eventPublishService;
    @Autowired
    private ConsumptionRollupService consumptionRollupService;
    @Autowired
    private TopicConsumerRepository topicConsumerRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventConsumptionRepository eventConsumptionRepository;

    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    @Qualifier("busConsumerQueue")
    private Queue busConsumerQueue;
    @Autowired
    private TopicExchange eventsExchange;
    @Autowired
    private Queue rollupQueue;

    @Autowired
    private TestBusEventConsumer testConsumer;
    @Autowired
    private ConsumptionFeedbackPort consumptionFeedbackPort;
    @Autowired
    private ObjectMapper objectMapper;

    private BusEventConsumerListener listener;

    @BeforeEach
    void setUp() {
        eventConsumptionRepository.deleteAll();
        eventRepository.deleteAll();
        topicConsumerRepository.deleteAll();

        amqpAdmin.declareExchange(eventsExchange);
        amqpAdmin.declareQueue(rollupQueue);
        amqpAdmin.declareQueue(busConsumerQueue);
        Binding binding = BindingBuilder.bind(busConsumerQueue).to(eventsExchange).with(TOPIC);
        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(busConsumerQueue.getName(), true);
        amqpAdmin.purgeQueue(rollupQueue.getName(), true);

        TopicConsumerDocument tc = new TopicConsumerDocument();
        tc.setTopic(TOPIC);
        tc.setConsumerId(CONSUMER_ID);
        tc.setEnabled(true);
        tc.setCreatedAt(Instant.now());
        tc.setUpdatedAt(Instant.now());
        topicConsumerRepository.save(tc);

        testConsumer.failNext.set(false);
        testConsumer.received.clear();

        listener = new BusEventConsumerListener(
            testConsumer,
            consumptionFeedbackPort,
            objectMapper,
            CONSUMER_ID
        );
    }

    private static EventEnvelope sampleEnvelope() {
        return new EventEnvelope(
            EVENT_ID,
            "trace-1",
            "span-1",
            null,
            TOPIC,
            "{\"orderId\":\"o1\"}",
            EventEnvelope.PAYLOAD_TYPE_JSON,
            new Initiator("order-service", "createOrder", "user-1", null),
            Instant.parse("2025-02-28T10:00:00Z"),
            null,
            null
        );
    }

    @Nested
    @DisplayName("消费成功")
    class Success {

        @Test
        @DisplayName("发布事件 -> 消费者收到 -> 更新待消费记录 success=true -> rollup 回写 events.status=CONSUMED")
        void publish_thenConsume_success_feedbackAndRollup() throws Exception {
            assertThat(eventPublishService.publish(sampleEnvelope()).success()).isTrue();

            Message mqMsg = rabbitTemplate.receive(busConsumerQueue.getName(), 5000);
            assertThat(mqMsg).isNotNull();

            listener.onMessage(mqMsg);

            EventEnvelope received = testConsumer.received.peek();
            assertThat(received).isNotNull();
            assertThat(received.eventId()).isEqualTo(EVENT_ID);
            assertThat(received.topic()).isEqualTo(TOPIC);

            List<EventConsumptionDocument> list =
                eventConsumptionRepository.findByEventIdAndConsumerIdOrderByAttemptNoDesc(EVENT_ID, CONSUMER_ID);
            assertThat(list).hasSize(1);
            EventConsumptionDocument latest = list.get(0);
            assertThat(latest.getAttemptNo()).isEqualTo(0);
            assertThat(latest.getSuccess()).isTrue();

            // 这里不强依赖 rollup 队列里是否还有消息（可能被监听器消费掉），直接调用 rollup 服务验证回写逻辑
            consumptionRollupService.rollupAndWriteBack(EVENT_ID);

            EventDocument event = eventRepository.findById(EVENT_ID).orElseThrow();
            assertThat(event.getStatus()).isEqualTo(EventStatus.CONSUMED.name());
        }
    }

    @Nested
    @DisplayName("消费失败")
    class Failure {

        @Test
        @DisplayName("消费抛异常 -> 更新待消费记录 success=false -> rollup 回写 events.status=FAILED（不 requeue）")
        void publish_thenConsume_failure_feedbackAndRollup() throws Exception {
            testConsumer.failNext.set(true);
            assertThat(eventPublishService.publish(sampleEnvelope()).success()).isTrue();

            Message mqMsg = rabbitTemplate.receive(busConsumerQueue.getName(), 5000);
            assertThat(mqMsg).isNotNull();

            assertThatThrownBy(() -> listener.onMessage(mqMsg))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(FAILURE_MESSAGE);

            EventEnvelope received = testConsumer.received.peek();
            assertThat(received).isNotNull();

            List<EventConsumptionDocument> list =
                eventConsumptionRepository.findByEventIdAndConsumerIdOrderByAttemptNoDesc(EVENT_ID, CONSUMER_ID);
            assertThat(list).hasSize(1);
            EventConsumptionDocument latest = list.get(0);
            assertThat(latest.getAttemptNo()).isEqualTo(0);
            assertThat(latest.getSuccess()).isFalse();
            assertThat(latest.getErrorMessage()).isEqualTo(FAILURE_MESSAGE);
            assertThat(latest.getErrorCode()).isEqualTo(RuntimeException.class.getSimpleName());

            consumptionRollupService.rollupAndWriteBack(EVENT_ID);

            EventDocument event = eventRepository.findById(EVENT_ID).orElseThrow();
            assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED.name());
        }
    }

    @TestConfiguration
    static class TestConsumerConfig {

        @Bean
        @Primary
        TestBusEventConsumer testBusEventConsumer() {
            return new TestBusEventConsumer();
        }
    }

    /**
     * 测试用消费者实现：记录收到的 envelope；可配置下一次消费是否抛异常。
     */
    static class TestBusEventConsumer implements BusEventConsumer {
        final BlockingQueue<EventEnvelope> received = new LinkedBlockingQueue<>();
        final AtomicBoolean failNext = new AtomicBoolean(false);

        @Override
        public void onEvent(EventEnvelope envelope) {
            received.offer(envelope);
            if (failNext.get()) {
                throw new RuntimeException(FAILURE_MESSAGE);
            }
        }
    }
}
