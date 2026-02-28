package com.wx.man.api.dto;

import java.time.Instant;

/**
 * 单条消费反馈记录。
 */
public record EventConsumptionItemDto(
    String id,
    String consumerId,
    int attemptNo,
    Boolean success,
    Instant consumedAt,
    String errorMessage,
    String errorCode,
    Instant createdAt
) {}
