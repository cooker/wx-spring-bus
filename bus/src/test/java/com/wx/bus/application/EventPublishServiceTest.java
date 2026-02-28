package com.wx.bus.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wx.bus.BusTestApplication;
import com.wx.bus.TestRabbitConfig;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.Initiator;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.bus.infrastructure.mongo.TopicConsumerDocument;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;

/**
 * 事件发送功能集成测试：使用本机 MongoDB、RabbitMQ，通过测试配置文件 {@code application.yml} 指定测试库与连接，验证先落库后发 MQ、无 topic_consumers 时跳过。
 * <p>运行前需本机已启动 MongoDB、RabbitMQ。</p>
 */
@SpringBootTest(classes = BusTestApplication.class)
@ActiveProfiles("test")
class EventPublishServiceTest {

    @Autowired
    private EventPublishService eventPublishService;
    @Autowired
    private TopicConsumerRepository topicConsumerRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventConsumptionRepository eventConsumptionRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    @Qualifier("testOrderPurchasedQueue")
    private Queue testOrderPurchasedQueue;
    @Autowired
    @Qualifier("testOrderPurchasedBinding")
    private Binding testOrderPurchasedBinding;

    private static final String EVENT_ID = UUID.randomUUID().toString();
    private static final String TOPIC = "order.purchased";
    private static final String PAYLOAD = "{\"orderId\":\"o1\"}";

    private EventEnvelope envelope;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        eventConsumptionRepository.deleteAll();
        topicConsumerRepository.deleteAll();

        if (rabbitAdmin != null && testOrderPurchasedQueue != null && testOrderPurchasedBinding != null) {
            rabbitAdmin.declareQueue(testOrderPurchasedQueue);
            rabbitAdmin.declareBinding(testOrderPurchasedBinding);
            rabbitAdmin.purgeQueue(testOrderPurchasedQueue.getName(), true);
        }

        envelope = new EventEnvelope(
            EVENT_ID,
            "trace-1",
            "span-1",
            null,
            TOPIC,
            PAYLOAD,
            EventEnvelope.PAYLOAD_TYPE_JSON,
            new Initiator("order-service", "createOrder", "user-1", null),
            Instant.parse("2025-02-28T10:00:00Z"),
            null,
            null
        );
    }

    @Nested
    @DisplayName("publish - 正常发送")
    class PublishSuccess {

        @Test
        @DisplayName("存在 topic_consumers 时：落库、初始化 consumptions、发 MQ、更新 SENT、返回 ok")
        void shouldSaveEventAndConsumptionsPublishMqAndReturnOk() throws Exception {
            TopicConsumerDocument tc1 = new TopicConsumerDocument();
            tc1.setTopic(TOPIC);
            tc1.setConsumerId("member-service");
            tc1.setEnabled(true);
            tc1.setCreatedAt(Instant.now());
            tc1.setUpdatedAt(Instant.now());
            topicConsumerRepository.save(tc1);

            TopicConsumerDocument tc2 = new TopicConsumerDocument();
            tc2.setTopic(TOPIC);
            tc2.setConsumerId("notification-service");
            tc2.setEnabled(true);
            tc2.setCreatedAt(Instant.now());
            tc2.setUpdatedAt(Instant.now());
            topicConsumerRepository.save(tc2);

            PublishResult result = eventPublishService.publish(envelope);

            assertThat(result.success()).isTrue();
            assertThat(result.eventId()).isEqualTo(EVENT_ID);
            assertThat(result.message()).isNull();

            // 验证 events 库：有一条事件且状态为 SENT
            EventDocument event = eventRepository.findById(EVENT_ID).orElse(null);
            assertThat(event).isNotNull();
            assertThat(event.getId()).isEqualTo(EVENT_ID);
            assertThat(event.getTopic()).isEqualTo(TOPIC);
            assertThat(event.getStatus()).isEqualTo("SENT");
            assertThat(event.getPayload()).isNotNull();

            // 验证 event_consumptions：每个消费者一条，attemptNo=0，success=null
            List<EventConsumptionDocument> consumptions = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(EVENT_ID);
            assertThat(consumptions).hasSize(2);
            assertThat(consumptions).extracting(EventConsumptionDocument::getEventId).containsOnly(EVENT_ID);
            assertThat(consumptions).extracting(EventConsumptionDocument::getConsumerId)
                .containsExactlyInAnyOrder("member-service", "notification-service");
            assertThat(consumptions).extracting(EventConsumptionDocument::getAttemptNo).containsOnly(0);
            assertThat(consumptions).extracting(EventConsumptionDocument::getSuccess).containsOnly((Boolean) null);

            // 验证 MQ：测试队列能收到一条消息，body 为信封 JSON
            Message mqMsg = rabbitTemplate.receive(TestRabbitConfig.TEST_QUEUE_ORDER_PURCHASED, TimeUnit.SECONDS.toMillis(5));
            assertThat(mqMsg).isNotNull();
            String body = new String(mqMsg.getBody(), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            assertThat(node.get("eventId").asText()).isEqualTo(EVENT_ID);
            assertThat(node.get("topic").asText()).isEqualTo(TOPIC);
        }
    }

    @Nested
    @DisplayName("publish - 无 topic_consumers 时跳过")
    class PublishSkippedWhenNoConsumers {

        @Test
        @DisplayName("无启用消费者时返回 skipped，不写库不发 MQ")
        void whenConsumersEmpty_shouldReturnSkippedAndNotSave() {
            // 不插入任何 topic_consumers，或只插入 enabled=false
            TopicConsumerDocument tc = new TopicConsumerDocument();
            tc.setTopic(TOPIC);
            tc.setConsumerId("member-service");
            tc.setEnabled(false);
            tc.setCreatedAt(Instant.now());
            tc.setUpdatedAt(Instant.now());
            topicConsumerRepository.save(tc);

            PublishResult result = eventPublishService.publish(envelope);

            assertThat(result.success()).isFalse();
            assertThat(result.eventId()).isEqualTo(EVENT_ID);
            assertThat(result.message()).contains("No enabled consumers");

            assertThat(eventRepository.findById(EVENT_ID)).isEmpty();
            assertThat(eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(EVENT_ID)).isEmpty();

            Message mqMsg = rabbitTemplate.receive(TestRabbitConfig.TEST_QUEUE_ORDER_PURCHASED, 500);
            assertThat(mqMsg).isNull();
        }
    }

    @Nested
    @DisplayName("buildEnvelope - 静态工厂")
    class BuildEnvelope {

        @Test
        @DisplayName("eventId 为空时生成 UUID")
        void whenEventIdBlank_shouldGenerateUuid() {
            EventEnvelope built = EventPublishService.buildEnvelope(
                null, null, null, null, TOPIC, PAYLOAD, null, null, null, null
            );
            assertThat(built.eventId()).isNotBlank();
            assertThat(built.eventId()).matches("^[0-9a-f-]{36}$");
            assertThat(built.topic()).isEqualTo(TOPIC);
            assertThat(built.payloadType()).isEqualTo(EventEnvelope.PAYLOAD_TYPE_JSON);
        }

        @Test
        @DisplayName("eventId 已提供时原样使用")
        void whenEventIdProvided_shouldUseIt() {
            EventEnvelope built = EventPublishService.buildEnvelope(
                EVENT_ID, null, null, null, TOPIC, PAYLOAD, null, null, null, null
            );
            assertThat(built.eventId()).isEqualTo(EVENT_ID);
        }

        @Test
        @DisplayName("occurredAt 为 null 时使用当前时间")
        void whenOccurredAtNull_shouldUseNow() {
            EventEnvelope built = EventPublishService.buildEnvelope(
                EVENT_ID, null, null, null, TOPIC, PAYLOAD, null, null, null, null
            );
            assertThat(built.occurredAt()).isNotNull();
        }
    }
}
