package com.wx.man.api.dto;

import java.util.List;

/**
 * 首页概览统计 DTO。
 * <p>
 * 用于首页展示指定日期的整体事件情况，包括处理中数量、总事件数、Topic/消费者数量以及 0–23 点每小时的事件数。
 * </p>
 *
 * @param date                 统计日期，格式 {@code yyyy-MM-dd}
 * @param todayProcessingCount 该日期内「处理中」事件数量（PENDING/SENT/PARTIAL/RETRYING）
 * @param totalEventCount      该日期内事件总数（不区分状态）
 * @param topicCount           系统已配置的 Topic 总数
 * @param consumerCount        已配置的消费者总数
 * @param hourlyCounts         长度为 24 的列表，下标为小时（0–23），值为该小时的事件数量
 */
public record HomeStatsDto(
    String date,
    long todayProcessingCount,
    long totalEventCount,
    long topicCount,
    long consumerCount,
    List<Long> hourlyCounts
) {}
