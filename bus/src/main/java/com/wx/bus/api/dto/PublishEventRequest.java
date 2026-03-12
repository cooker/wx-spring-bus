package com.wx.bus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 发布事件请求体；与 {@code EventEnvelope} 字段一一对应。
 * <p>
 * 一般由业务方通过 REST 或门面调用，用于向事件总线发起「业务事件」。
 * {@code eventId} / {@code occurredAt} 可为空，由服务端在落库时补全。
 * </p>
 *
 * @param eventId        事件 ID，可为空；为空时由服务端生成全局唯一 ID
 * @param traceId        链路 ID，用于在调用链中串联多个事件
 * @param spanId         当前 span ID，可选
 * @param parentEventId  父事件 ID，用于事件树/链路追踪
 * @param topic          事件主题（必填），决定路由与消费者列表
 * @param payload        事件载荷对象（必填），通常为业务 JSON 对象或 Map
 * @param payloadType    载荷类型标识，可选，默认视为 JSON
 * @param initiator      事件发起方信息（服务名、操作名、用户等）
 * @param occurredAt     业务发生时间，可为空；为空时视为当前时间
 * @param expireAt       事件过期时间，可选，仅在需要 TTL / 失效控制时设置
 */
public record PublishEventRequest(
    String eventId,
    String traceId,
    String spanId,
    String parentEventId,
    @NotBlank String topic,
    @NotNull Object payload,
    String payloadType,
    InitiatorDto initiator,
    Instant occurredAt,
    Instant expireAt
) {}
