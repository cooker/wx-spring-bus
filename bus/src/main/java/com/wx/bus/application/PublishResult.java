package com.wx.bus.application;

/**
 * 发布结果：不抛异常时通过此返回调用方，不影响主流程。
 *
 * @param success 是否已成功落库并发布 MQ
 * @param eventId 事件 ID（无论成功与否均可能返回）
 * @param message 失败或跳过时的说明，如无 topic_consumers 配置、MQ 发布失败
 */
public record PublishResult(boolean success, String eventId, String message) {

    /** 发布成功 */
    public static PublishResult ok(String eventId) {
        return new PublishResult(true, eventId, null);
    }

    /** 未发送（如无配置、MQ 失败），调用方可根据 message 处理 */
    public static PublishResult skipped(String eventId, String message) {
        return new PublishResult(false, eventId, message);
    }
}
