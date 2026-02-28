package com.wx.bus.api;

import com.wx.bus.api.dto.PublishEventRequest;
import com.wx.bus.application.EventPublishService;
import com.wx.bus.application.PublishResult;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.event.EventPublishRequestEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件发布公共门面：通过 Spring 事件触发发布，无 REST。
 * <p>调用方注入本类后调用 {@link #publish(EventEnvelope)} 或 {@link #publish(PublishEventRequest)}，内部发布 {@link EventPublishRequestEvent}，由监听器执行落库与发 MQ。</p>
 */
@Component
public class WxBusEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public WxBusEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布事件（信封）。同步等待监听器执行完毕后返回结果。
     */
    public PublishResult publish(EventEnvelope envelope) {
        EventPublishRequestEvent event = new EventPublishRequestEvent(this, envelope);
        eventPublisher.publishEvent(event);
        PublishResult result = event.getResult();
        return result != null ? result : PublishResult.skipped(envelope.eventId(), "Publish not completed");
    }

    /**
     * 发布事件（请求体）。根据 request 构建信封后发布，eventId/occurredAt 可空由服务端补全。
     */
    public PublishResult publish(PublishEventRequest request) {
        EventEnvelope envelope = EventPublishService.buildEnvelope(
            request.eventId(),
            request.traceId(),
            request.spanId(),
            request.parentEventId(),
            request.topic(),
            request.payload(),
            request.payloadType(),
            request.initiator() != null ? request.initiator().toInitiator() : null,
            request.occurredAt(),
            request.expireAt()
        );
        return publish(envelope);
    }
}
