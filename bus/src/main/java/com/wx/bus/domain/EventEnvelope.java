package com.wx.bus.domain;

import java.time.Instant;

/**
 * 事件信封（与设计文档 1.2 一致），用于 RabbitMQ 消息体与 MongoDB 持久化。
 *
 * @param eventId       全局唯一事件 ID，用于去重、重试、关联
 * @param traceId      分布式链路 ID，同一次业务触发的多事件共享
 * @param spanId       当前环节 span ID
 * @param parentEventId 若由上游事件消费触发则填上游 eventId
 * @param topic        逻辑主题，对应 MQ routing key，如 order.purchased
 * @param payload      业务载荷，JSON 对象或序列化字符串
 * @param payloadType  载荷类型，如 application/json
 * @param initiator    发起方信息
 * @param occurredAt   事件发生时间
 * @param sentAt       首次发送到 MQ 的时间（发送时由 bus 写入）
 * @param expireAt     业务过期时间，可选
 */
public record EventEnvelope(
    String eventId,
    String traceId,
    String spanId,
    String parentEventId,
    String topic,
    Object payload,
    String payloadType,
    Initiator initiator,
    Instant occurredAt,
    Instant sentAt,
    Instant expireAt
) {
    /** 默认 JSON 载荷类型 */
    public static final String PAYLOAD_TYPE_JSON = "application/json";
}
