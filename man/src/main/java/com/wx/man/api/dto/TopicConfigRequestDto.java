package com.wx.man.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Topic 配置 创建/更新 请求体。
 */
public record TopicConfigRequestDto(
    @NotBlank(message = "topic 不能为空")
    @Size(max = 256)
    String topic,

    @Size(max = 128)
    String nameZh,

    @Size(max = 512)
    String description
) {}
