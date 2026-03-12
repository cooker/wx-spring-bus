package com.wx.man.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Topic–消费者配置创建/更新请求体。
 *
 * @param topic       Topic 名称（必填），长度 &lt;= 256
 * @param consumerId  消费者 ID（必填），长度 &lt;= 256；需与实际消费者实现中的 ID 对应
 * @param enabled     是否启用该消费者；禁用后不会再参与发送校验和初始化
 * @param sortOrder   排序权重，可选；用于管理端列表展示顺序
 * @param description 配置说明，长度 &lt;= 512，可选
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
