package com.wx.bus.api.dto;

/**
 * 发布事件响应。
 * <p>
 * 用于告知调用方本次发布是否成功被受理：即事件已写入 {@code events} 并（在有消费者配置时）发送到 MQ。
 * 若 {@code success=false}，{@code message} 中会给出失败或跳过的原因（例如：无启用的 {@code topic_consumers} 配置）。
 * </p>
 *
 * @param success 是否受理成功（已落库，且在有消费者时已发 MQ）
 * @param eventId 事件 ID；成功时一定存在，失败时可能为空
 * @param message 失败或跳过原因说明；成功时通常为 {@code null}
 */
public record PublishEventResponse(boolean success, String eventId, String message) {

    public static PublishEventResponse ok(String eventId) {
        return new PublishEventResponse(true, eventId, null);
    }

    public static PublishEventResponse skipped(String eventId, String message) {
        return new PublishEventResponse(false, eventId, message);
    }
}
