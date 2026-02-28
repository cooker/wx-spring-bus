package com.wx.bus.event;

import com.wx.bus.application.PublishResult;
import com.wx.bus.domain.EventEnvelope;
import org.springframework.context.ApplicationEvent;

/**
 * 事件发布请求的 Spring 事件：门面 {@link com.wx.bus.api.WxBusEventPublisher} 发布此事件，监听器执行实际落库与发 MQ 并回写结果。
 */
public class EventPublishRequestEvent extends ApplicationEvent {

    private final EventEnvelope envelope;
    private volatile PublishResult result;

    public EventPublishRequestEvent(Object source, EventEnvelope envelope) {
        super(source);
        this.envelope = envelope;
    }

    public EventEnvelope getEnvelope() {
        return envelope;
    }

    public void setResult(PublishResult result) {
        this.result = result;
    }

    public PublishResult getResult() {
        return result;
    }
}
