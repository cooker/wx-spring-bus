package com.wx.man.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 事件列表项。
 */
public record EventListItemDto(
    String eventId,
    String parentEventId,
    String topic,
    String status,
    Instant occurredAt,
    Instant statusAt,
    int retryCount,
    Map<String, String> initiator
) {}
