package com.wx.bus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 消费反馈请求体。
 * <p>
 * 每次消费者对某条事件的消费结果回调，都对应一条 {@code event_consumptions} 记录。
 * {@code attemptNo} 由服务端根据 (eventId, consumerId) 当前最大值自动递增，调用方无需关心。
 * </p>
 *
 * @param eventId      事件 ID（必填），与 {@code events.eventId} 对应
 * @param consumerId   消费者 ID（必填），通常为业务服务或消费逻辑的唯一标识
 * @param success      本次消费是否成功（必填）
 * @param consumedAt   实际消费完成时间，可为空；为空时服务端使用当前时间
 * @param errorMessage 失败时的错误信息，可选；成功时可为空
 * @param errorCode    失败时的错误码或异常类型标识，可选
 */
public record ConsumptionFeedbackRequest(
    @NotBlank String eventId,
    @NotBlank String consumerId,
    @NotNull Boolean success,
    Instant consumedAt,
    String errorMessage,
    String errorCode
) {}
