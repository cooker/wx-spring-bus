package com.wx.bus.api;

import com.wx.bus.application.port.ConsumptionFeedbackPort;
import com.wx.bus.event.ConsumptionFeedbackEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 消费反馈公共门面：通过 Spring 事件触发反馈写入，无 REST。
 * <p>调用方注入本类后调用 {@link #recordFeedback}，内部发布 {@link ConsumptionFeedbackEvent}，由监听器写入 event_consumptions 并触发汇总回写。</p>
 */
@Component
public class WxBusConsumptionFeedback implements ConsumptionFeedbackPort {

    private final ApplicationEventPublisher eventPublisher;

    public WxBusConsumptionFeedback(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void recordFeedback(String eventId,
                              String consumerId,
                              boolean success,
                              Instant consumedAt,
                              String errorMessage,
                              String errorCode) {
        eventPublisher.publishEvent(new ConsumptionFeedbackEvent(
            this, eventId, consumerId, success, consumedAt, errorMessage, errorCode
        ));
    }
}
