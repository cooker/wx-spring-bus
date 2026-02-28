package com.wx.man.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Topic–消费者配置 创建/更新 请求体。
 */
public record TopicConsumerRequestDto(
    @NotBlank(message = "topic 不能为空")
    @Size(max = 256)
    String topic,

    @NotBlank(message = "consumerId 不能为空")
    @Size(max = 256)
    String consumerId,

    boolean enabled,

    Integer sortOrder,

    @Size(max = 512)
    String description
) {}
