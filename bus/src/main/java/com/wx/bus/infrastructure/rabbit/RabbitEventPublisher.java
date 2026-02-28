package com.wx.bus.infrastructure.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wx.bus.application.port.EventPublisherPort;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 将事件信封发布到 RabbitMQ topic 交换机。
 * <p>routing key = envelope.topic()，消息体为 envelope 的 JSON 序列化。</p>
 */
@Component
public class RabbitEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String eventsExchangeName;

    public RabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        ObjectMapper objectMapper,
        @Value("${bus.mq.events-exchange:bus.events}") String eventsExchangeName
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.eventsExchangeName = eventsExchangeName;
    }

    @Override
    public void publish(EventEnvelope envelope) {
        String routingKey = envelope.topic();
        String body;
        try {
            body = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event envelope eventId={} userId={}", envelope.eventId(), LogContext.getUserId(), e);
            throw new RuntimeException("Event serialization failed", e);
        }
        rabbitTemplate.convertAndSend(eventsExchangeName, routingKey, body);
        log.debug("Published event eventId={} topic={} userId={}", envelope.eventId(), routingKey, LogContext.getUserId());
    }
}
