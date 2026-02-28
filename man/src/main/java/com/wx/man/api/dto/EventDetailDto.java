package com.wx.man.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 事件详情（含消费记录）。
 */
public record EventDetailDto(
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
    List<EventConsumptionItemDto> consumptions
) {}
