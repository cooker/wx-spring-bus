package com.wx.bus.infrastructure.rabbit;

import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 向消费汇总专用队列投递 eventId，触发异步回写 events.status。
 */
@Component
public class RollupPublisher {

    private static final Logger log = LoggerFactory.getLogger(RollupPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String rollupQueueName;

    public RollupPublisher(
        RabbitTemplate rabbitTemplate,
        @Value("${bus.mq.rollup-queue:bus.consumption-rollup}") String rollupQueueName
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rollupQueueName = rollupQueueName;
    }

    /**
     * 向消费汇总队列发送 eventId，由 {@link RollupListener} 串行消费并回写 events.status。
     */
    public void sendEventIdForRollup(String eventId) {
        rabbitTemplate.convertAndSend(rollupQueueName, eventId);
        log.debug("Sent eventId to rollup queue eventId={} userId={}", eventId, LogContext.getUserId());
    }
}
