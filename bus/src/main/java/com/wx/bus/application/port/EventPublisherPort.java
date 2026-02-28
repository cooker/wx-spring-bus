package com.wx.bus.application.port;

import com.wx.bus.domain.EventEnvelope;

/**
 * 事件发往 MQ 的端口（应用层依赖接口，便于测试 mock）。
 */
public interface EventPublisherPort {

    /**
     * 将事件信封发布到 MQ。
     */
    void publish(EventEnvelope envelope);
}
