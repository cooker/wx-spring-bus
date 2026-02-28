package com.wx.bus.infrastructure.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wx.bus.application.port.BusEventConsumer;
import com.wx.bus.application.port.ConsumptionFeedbackPort;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 业务事件消费者监听器：从消费者队列拉取消息，反序列化为 {@link EventEnvelope}，调用 {@link BusEventConsumer}，并根据结果上报消费反馈。
 * <p>仅在配置了 bus.consumer.consumer-id 且存在 {@link BusEventConsumer} Bean 时生效。</p>
 */
@Component
@ConditionalOnBean(BusEventConsumer.class)
@ConditionalOnProperty(prefix = "bus.consumer", name = "consumer-id")
public class BusEventConsumerListener {

    private static final Logger log = LoggerFactory.getLogger(BusEventConsumerListener.class);

    private final BusEventConsumer busEventConsumer;
    private final ConsumptionFeedbackPort consumptionFeedback;
    private final ObjectMapper objectMapper;
    private final String consumerId;

    public BusEventConsumerListener(
        BusEventConsumer busEventConsumer,
        ConsumptionFeedbackPort consumptionFeedback,
        ObjectMapper objectMapper,
        @Value("${bus.consumer.consumer-id}") String consumerId
    ) {
        this.busEventConsumer = busEventConsumer;
        this.consumptionFeedback = consumptionFeedback;
        this.objectMapper = objectMapper;
        this.consumerId = consumerId;
    }

    @RabbitListener(queues = "#{busConsumerQueue.name}")
    public void onMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, EventEnvelope.class);
        } catch (Exception e) {
            log.error("Failed to deserialize event body consumerId={} userId={}", consumerId, LogContext.getUserId(), e);
            throw new RuntimeException("Event deserialization failed", e);
        }
        String eventId = envelope.eventId();
        Instant consumedAt = Instant.now();
        try {
            busEventConsumer.onEvent(envelope);
            consumptionFeedback.recordFeedback(eventId, consumerId, true, consumedAt, null, null);
        } catch (Exception e) {
            log.warn("Consumer failed eventId={} consumerId={} userId={}", eventId, consumerId, LogContext.getUserId(), e);
            consumptionFeedback.recordFeedback(
                eventId, consumerId, false, consumedAt,
                e.getMessage(), e.getClass().getSimpleName()
            );
            throw e;
        }
    }
}
