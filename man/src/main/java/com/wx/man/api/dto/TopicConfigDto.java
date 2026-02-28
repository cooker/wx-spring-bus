package com.wx.man.api.dto;

import java.time.Instant;

/**
 * Topic 配置（列表/详情响应）。
 */
public record TopicConfigDto(
    String id,
    String topic,
    String nameZh,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
