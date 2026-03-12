package com.wx.man.api.dto;

import java.time.Instant;

/**
 * Topic–消费者关联配置 DTO（列表/详情响应）。
 *
 * @param id          配置 ID
 * @param topic       事件 topic 名称
 * @param consumerId  消费者 ID（业务方自定义，需在消费端与此保持一致）
 * @param enabled     是否启用该消费者；仅启用的配置会参与发送校验与初始化消费记录
 * @param sortOrder   排序权重，用于在管理端列表中排序，可选
 * @param description 配置说明，可选
 * @param createdAt   配置创建时间
 * @param updatedAt   配置最后修改时间
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
