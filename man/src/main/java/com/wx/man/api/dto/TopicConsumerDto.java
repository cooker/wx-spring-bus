package com.wx.man.api.dto;

import java.time.Instant;

/**
 * Topic–消费者配置（列表/详情响应）。
 */
public record TopicConsumerDto(
    String id,
    String topic,
    String consumerId,
    boolean enabled,
    Integer sortOrder,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
