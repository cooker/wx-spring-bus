package com.wx.bus.domain;

/**
 * 事件状态（与设计文档 2.4 一致）。
 * <ul>
 *   <li>{@link #PENDING} 已创建未发送（先落库后发 MQ 时，写库后、发 MQ 前）</li>
 *   <li>{@link #SENT} 已发送到 MQ，尚未确认消费结果</li>
 *   <li>{@link #CONSUMED} 已成功消费（单/多消费者均成功）</li>
 *   <li>{@link #PARTIAL} 多消费者场景下部分成功、部分失败</li>
 *   <li>{@link #FAILED} 消费或发送失败，可人工重推</li>
 *   <li>{@link #RETRYING} 正在重试（已再次投递 MQ）</li>
 *   <li>{@link #EXPIRED} 已过期</li>
 * </ul>
 */
public enum EventStatus {

    /** 已创建未发送 */
    PENDING,
    /** 已发送到 RabbitMQ */
    SENT,
    /** 已成功消费 */
    CONSUMED,
    /** 多消费者部分成功 */
    PARTIAL,
    /** 失败，可重推 */
    FAILED,
    /** 重试中 */
    RETRYING,
    /** 已过期 */
    EXPIRED
}
