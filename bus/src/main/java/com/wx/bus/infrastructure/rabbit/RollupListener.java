package com.wx.bus.infrastructure.rabbit;

import com.wx.bus.application.ConsumptionRollupService;
import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 消费汇总专用队列的单一消费者，串行处理 eventId 并回写 events.status。
 */
@Component
public class RollupListener {

    private static final Logger log = LoggerFactory.getLogger(RollupListener.class);

    private final ConsumptionRollupService rollupService;
    private final String rollupQueueName;

    public RollupListener(
        ConsumptionRollupService rollupService,
        @Value("${bus.mq.rollup-queue:bus.consumption-rollup}") String rollupQueueName
    ) {
        this.rollupService = rollupService;
        this.rollupQueueName = rollupQueueName;
    }

    /**
     * 消费汇总队列消息：消息体为 eventId，串行执行汇总并回写 events。
     */
    @RabbitListener(queues = "${bus.mq.rollup-queue:bus.consumption-rollup}")
    public void onRollupMessage(String eventId) {
        try {
            rollupService.rollupAndWriteBack(eventId);
        } catch (Exception e) {
            log.error("Rollup failed for eventId={} userId={}", eventId, LogContext.getUserId(), e);
            throw e; /* 抛出以触发 AMQP 重试或进入 DLQ */
        }
    }
}
