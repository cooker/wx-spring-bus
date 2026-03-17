package com.wx.man.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 事件详情（响应），包含事件主信息及所有消费记录。
 *
 * @param eventId      事件 ID
 * @param traceId      链路 ID，用于跨服务追踪
 * @param spanId       当前 span ID
 * @param parentEventId 父事件 ID，用于事件树/调用链展示
 * @param topic        事件主题
 * @param payload      事件载荷内容，通常为 JSON 反序列化后的对象/Map
 * @param payloadType  载荷类型标识，如 {@code application/json}
 * @param initiator    发起方信息 Map（service/operation/userId/clientRequestId）
 * @param occurredAt   业务发生时间
 * @param sentAt       首次成功发送到 MQ 的时间
 * @param expireAt     事件过期时间（如配置了 TTL），可能为空
 * @param status       事件当前状态：PENDING/SENT/CONSUMED/PARTIAL/FAILED 等
 * @param statusAt     当前状态变更时间
 * @param retryCount   已重试发送次数
 * @param lastSentAt   最近一次发送到 MQ 的时间
 * @param createdAt    文档创建时间
 * @param updatedAt    文档最后更新时间
 * @param consumptions 该事件下所有消费者的消费记录列表
 */
public record EventDetailResponse(
    String eventId,
    String traceId,
    String spanId,
    String parentEventId,
    String topic,
    Object payload,
    String payloadType,
    Map<String, String> initiator,
    Instant occurredAt,
    Instant sentAt,
    Instant expireAt,
    String status,
    Instant statusAt,
    int retryCount,
    Instant lastSentAt,
    Instant createdAt,
    Instant updatedAt,
    List<EventConsumptionItemResponse> consumptions
) {}
