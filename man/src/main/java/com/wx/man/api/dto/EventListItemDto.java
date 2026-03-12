package com.wx.man.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 事件列表行 DTO，用于事件列表接口的单条记录展示。
 *
 * @param eventId       事件 ID
 * @param parentEventId 父事件 ID，用于链路/树状展示，没有父事件时为空
 * @param topic         事件主题
 * @param status        事件当前状态，如 PENDING/SENT/CONSUMED/FAILED 等
 * @param occurredAt    业务发生时间
 * @param statusAt      当前状态变更时间
 * @param retryCount    已重试发送次数
 * @param initiator     发起方信息（service/operation/userId/clientRequestId），以 Map 形式返回便于前端展示
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
