package com.wx.man.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Topic 配置创建/更新请求体。
 *
 * @param topic       Topic 名称（必填），长度 &lt;= 256；用于唯一标识事件主题
 * @param nameZh      Topic 中文名称，长度 &lt;= 128，可选
 * @param description 说明文字，长度 &lt;= 512，可选
 */
public record TopicConfigRequest(
    @NotBlank(message = "topic 不能为空")
    @Size(max = 256)
    String topic,

    @Size(max = 128)
    String nameZh,

    @Size(max = 512)
    String description
) {}
