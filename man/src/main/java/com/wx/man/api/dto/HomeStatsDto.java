package com.wx.man.api.dto;

import java.util.List;

/**
 * 首页统计：指定日期的处理中事件数、当日事件总数、Topic 总数、消费者总数、0-23 时每小时事件数。
 *
 * @param date 统计日期，yyyy-MM-dd
 */
public record HomeStatsDto(
    String date,
    long todayProcessingCount,
    long totalEventCount,
    long topicCount,
    long consumerCount,
    List<Long> hourlyCounts
) {}
