package com.wx.bus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 发布事件请求体；与 EventEnvelope 字段对应，eventId/occurredAt 可空由服务端补全。
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
