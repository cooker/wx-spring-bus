package com.wx.bus.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.wx.bus.BusTestApplication;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.Initiator;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.TopicConsumerDocument;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;

/**
 * 事件场景集成测试：模拟「用户A 下单成功」后的完整事件与消费链路。
 * <ol>
 *   <li>用户A 下单成功，发送事件 E-1（通知下单成功）</li>
 *   <li>会员服务 C1、消息服务 C2 接收到 E-1</li>
 *   <li>会员服务 C1 消费 E-1：积分增加成功，发送事件 E-2（通知新增抽奖次数）</li>
 *   <li>会员服务 C1 消费 E-2：新增抽奖次数成功</li>
 *   <li>消息服务 C2 消费 E-1：通知成功，发送事件 E-3（通知发送微信、短信等多种消息）</li>
 *   <li>消息服务 C2 消费 E-3：循环打印发送xx消息</li>
 * </ol>
 */
@SpringBootTest(classes = BusTestApplication.class)
@ActiveProfiles("test")
class EventScenarioTest {

    private static final String TOPIC_ORDER_SUCCESS = "order.purchased";
    private static final String TOPIC_LOTTERY_ADDED = "member.lottery.added";
    private static final String TOPIC_MESSAGE_MULTI = "message.multi.send";
    private static final String CONSUMER_MEMBER = "member-service";
    private static final String CONSUMER_MESSAGE = "message-service";

    @Autowired
    private EventPublishService eventPublishService;
    @Autowired
    private ConsumptionFeedbackService consumptionFeedbackService;
    @Autowired
    private TopicConsumerRepository topicConsumerRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventConsumptionRepository eventConsumptionRepository;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    @Qualifier("testOrderPurchasedQueue")
    private Queue testOrderPurchasedQueue;
    @Autowired
    @Qualifier("testOrderPurchasedBinding")
    private Binding testOrderPurchasedBinding;
    @Autowired
    @Qualifier("testMemberLotteryQueue")
    private Queue testMemberLotteryQueue;
    @Autowired
    @Qualifier("testMemberLotteryBinding")
    private Binding testMemberLotteryBinding;
    @Autowired
    @Qualifier("testMessageMultiQueue")
    private Queue testMessageMultiQueue;
    @Autowired
    @Qualifier("testMessageMultiBinding")
    private Binding testMessageMultiBinding;

    private String traceId;
    private String e1Id;
    private String e2Id;
    private String e3Id;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        eventConsumptionRepository.deleteAll();
        topicConsumerRepository.deleteAll();

        traceId = "trace-" + UUID.randomUUID();
        e1Id = "E-1-" + UUID.randomUUID();
        e2Id = "E-2-" + UUID.randomUUID();
        e3Id = "E-3-" + UUID.randomUUID();

        if (rabbitAdmin != null) {
            declareAndPurge(testOrderPurchasedQueue, testOrderPurchasedBinding);
            declareAndPurge(testMemberLotteryQueue, testMemberLotteryBinding);
            declareAndPurge(testMessageMultiQueue, testMessageMultiBinding);
        }

        // topic_consumers: E-1 由 C1、C2 消费；E-2 由 C1 消费；E-3 由 C2 消费
        saveTopicConsumer(TOPIC_ORDER_SUCCESS, CONSUMER_MEMBER);
        saveTopicConsumer(TOPIC_ORDER_SUCCESS, CONSUMER_MESSAGE);
        saveTopicConsumer(TOPIC_LOTTERY_ADDED, CONSUMER_MEMBER);
        saveTopicConsumer(TOPIC_MESSAGE_MULTI, CONSUMER_MESSAGE);
    }

    private void declareAndPurge(Queue queue, Binding binding) {
        if (queue != null && binding != null) {
            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareBinding(binding);
            rabbitAdmin.purgeQueue(queue.getName(), true);
        }
    }

    private void saveTopicConsumer(String topic, String consumerId) {
        TopicConsumerDocument tc = new TopicConsumerDocument();
        tc.setTopic(topic);
        tc.setConsumerId(consumerId);
        tc.setEnabled(true);
        tc.setCreatedAt(Instant.now());
        tc.setUpdatedAt(Instant.now());
        topicConsumerRepository.save(tc);
    }

    @Test
    @DisplayName("用户A下单成功 -> E-1 -> C1/C2 消费 -> C1 发 E-2 并消费 -> C2 发 E-3 并消费")
    void orderSuccessEventFlow() {
        Instant occurredAt = Instant.parse("2025-02-28T10:00:00Z");
        Initiator userA = new Initiator("order-service", "createOrder", "user-A", null);

        // 1. 用户A 下单成功，发送事件 E-1（通知下单成功）
        EventEnvelope e1 = new EventEnvelope(
            e1Id, traceId, "span-e1", null, TOPIC_ORDER_SUCCESS,
            "{\"orderId\":\"o1\",\"userId\":\"user-A\"}", EventEnvelope.PAYLOAD_TYPE_JSON,
            userA, occurredAt, null, null
        );
        PublishResult r1 = eventPublishService.publish(e1);
        assertThat(r1.success()).isTrue();
        assertThat(r1.eventId()).isEqualTo(e1Id);

        // 2. 会员服务 C1、消息服务 C2 接收到 E-1（落库并投递 MQ 后，consumptions 已初始化）
        EventDocument docE1 = eventRepository.findById(e1Id).orElse(null);
        assertThat(docE1).isNotNull();
        assertThat(docE1.getStatus()).isEqualTo("SENT");
        List<EventConsumptionDocument> consE1 = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(e1Id);
        assertThat(consE1).hasSize(2);
        assertThat(consE1).extracting(EventConsumptionDocument::getConsumerId)
            .containsExactlyInAnyOrder(CONSUMER_MEMBER, CONSUMER_MESSAGE);

        // 3. 会员服务 C1 消费 E-1：积分增加成功，发送事件 E-2（通知新增抽奖次数）
        consumptionFeedbackService.recordFeedback(e1Id, CONSUMER_MEMBER, true, Instant.now(), null, null);
        EventEnvelope e2 = new EventEnvelope(
            e2Id, traceId, "span-e2", e1Id, TOPIC_LOTTERY_ADDED,
            "{\"userId\":\"user-A\",\"lotteryCount\":1}", EventEnvelope.PAYLOAD_TYPE_JSON,
            new Initiator(CONSUMER_MEMBER, "onOrderPurchased", "user-A", null), Instant.now(), null, null
        );
        PublishResult r2 = eventPublishService.publish(e2);
        assertThat(r2.success()).isTrue();

        // 4. 会员服务 C1 消费 E-2：新增抽奖次数成功
        consumptionFeedbackService.recordFeedback(e2Id, CONSUMER_MEMBER, true, Instant.now(), null, null);
        List<EventConsumptionDocument> consE2 = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(e2Id);
        assertThat(consE2).hasSizeGreaterThanOrEqualTo(1);
        assertThat(consE2).extracting(EventConsumptionDocument::getConsumerId).containsOnly(CONSUMER_MEMBER);
        assertThat(consE2.stream().filter(c -> Boolean.TRUE.equals(c.getSuccess()))).isNotEmpty();

        // 5. 消息服务 C2 消费 E-1：通知成功，发送事件 E-3（通知发送微信、短信等多种消息）
        consumptionFeedbackService.recordFeedback(e1Id, CONSUMER_MESSAGE, true, Instant.now(), null, null);
        EventEnvelope e3 = new EventEnvelope(
            e3Id, traceId, "span-e3", e1Id, TOPIC_MESSAGE_MULTI,
            "{\"channels\":[\"wechat\",\"sms\"]}", EventEnvelope.PAYLOAD_TYPE_JSON,
            new Initiator(CONSUMER_MESSAGE, "onOrderPurchased", "user-A", null), Instant.now(), null, null
        );
        PublishResult r3 = eventPublishService.publish(e3);
        assertThat(r3.success()).isTrue();

        // 6. 消息服务 C2 消费 E-3：循环打印发送xx消息（此处仅记录消费成功）
        consumptionFeedbackService.recordFeedback(e3Id, CONSUMER_MESSAGE, true, Instant.now(), null, null);
        List<EventConsumptionDocument> consE3 = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(e3Id);
        assertThat(consE3).hasSizeGreaterThanOrEqualTo(1);
        assertThat(consE3).extracting(EventConsumptionDocument::getConsumerId).containsOnly(CONSUMER_MESSAGE);
        assertThat(consE3.stream().filter(c -> Boolean.TRUE.equals(c.getSuccess()))).isNotEmpty();

        // 汇总校验：三笔事件均存在，E-1 有两个消费者的反馈记录
        assertThat(eventRepository.findById(e1Id)).isPresent();
        assertThat(eventRepository.findById(e2Id)).isPresent();
        assertThat(eventRepository.findById(e3Id)).isPresent();
        List<EventConsumptionDocument> allE1 = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(e1Id);
        assertThat(allE1).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allE1.stream().filter(c -> c.getAttemptNo() > 0).map(EventConsumptionDocument::getSuccess))
            .contains(true);
    }
}
