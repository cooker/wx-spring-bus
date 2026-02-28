package com.wx.bus.event;

import com.wx.bus.application.EventPublishService;
import com.wx.bus.application.PublishResult;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 监听 {@link EventPublishRequestEvent}，调用 {@link EventPublishService#publish} 并将结果回写到事件对象。
 */
@Component
public class EventPublishListener {

    private final EventPublishService eventPublishService;

    public EventPublishListener(EventPublishService eventPublishService) {
        this.eventPublishService = eventPublishService;
    }

    @EventListener
    @Order(0)
    public void onEventPublishRequest(EventPublishRequestEvent event) {
        PublishResult result = eventPublishService.publish(event.getEnvelope());
        event.setResult(result);
    }
}
