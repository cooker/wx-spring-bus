package com.wx.man.api.dto;

import java.time.Instant;

/**
 * Topic 配置 DTO（列表/详情响应）。
 *
 * @param id          配置 ID
 * @param topic       事件 topic 名称（唯一标识某类事件）
 * @param nameZh      Topic 中文名称，便于在管理端展示
 * @param description Topic 说明文字（用途、触发场景等）
 * @param createdAt   配置创建时间
 * @param updatedAt   配置最后修改时间
 */
public record TopicConfigDto(
    String id,
    String topic,
    String nameZh,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
