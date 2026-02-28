package com.wx.bus.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * 消费反馈的 Spring 事件：门面发布此事件，监听器执行写入 event_consumptions 与触发汇总。
 */
public class ConsumptionFeedbackEvent extends ApplicationEvent {

    private final String eventId;
    private final String consumerId;
    private final boolean success;
    private final Instant consumedAt;
    private final String errorMessage;
    private final String errorCode;

    public ConsumptionFeedbackEvent(Object source,
                                    String eventId,
                                    String consumerId,
                                    boolean success,
                                    Instant consumedAt,
                                    String errorMessage,
                                    String errorCode) {
        super(source);
        this.eventId = eventId;
        this.consumerId = consumerId;
        this.success = success;
        this.consumedAt = consumedAt != null ? consumedAt : Instant.now();
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getEventId() { return eventId; }
    public String getConsumerId() { return consumerId; }
    public boolean isSuccess() { return success; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorCode() { return errorCode; }
}
