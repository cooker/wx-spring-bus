package com.wx.bus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 消费反馈请求体：一次回调对应一条 event_consumptions 记录（attemptNo 由服务端递增）。
 */
public record ConsumptionFeedbackRequest(
    @NotBlank String eventId,
    @NotBlank String consumerId,
    @NotNull Boolean success,
    Instant consumedAt,
    String errorMessage,
    String errorCode
) {}
