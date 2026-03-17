package com.wx.man.api.dto.response;

import java.time.Instant;

/**
 * 单条消费反馈记录（响应），对应 {@code event_consumptions} 集合中的一行。
 *
 * @param id           记录 ID（MongoDB 文档 ID）
 * @param consumerId   消费者 ID，对应消费该事件的业务消费者
 * @param attemptNo    第几次回调：0=初始化记录，1/2/3…=第 1/2/3 次实际消费回调
 * @param success      本次消费结果：{@code null}=待消费（初始化）、{@code true}=成功、{@code false}=失败
 * @param consumedAt   本次消费完成时间
 * @param errorMessage 失败时的错误信息
 * @param errorCode    失败时的错误码或异常类型
 * @param createdAt    该记录创建时间
 */
public record EventConsumptionItemResponse(
    String id,
    String consumerId,
    int attemptNo,
    Boolean success,
    Instant consumedAt,
    String errorMessage,
    String errorCode,
    Instant createdAt
) {}
