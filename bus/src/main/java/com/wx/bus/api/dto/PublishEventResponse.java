package com.wx.bus.api.dto;

/**
 * 发布事件响应：success 表示是否已落库并发布 MQ；未发送时 message 含原因（如无 topic_consumers 配置）。
 */
public record PublishEventResponse(boolean success, String eventId, String message) {

    public static PublishEventResponse ok(String eventId) {
        return new PublishEventResponse(true, eventId, null);
    }

    public static PublishEventResponse skipped(String eventId, String message) {
        return new PublishEventResponse(false, eventId, message);
    }
}
