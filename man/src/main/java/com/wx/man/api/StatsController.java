package com.wx.man.api;

import com.wx.bus.infrastructure.mongo.TopicConfigRepository;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;
import com.wx.man.api.dto.HomeStatsDto;
import com.wx.man.application.EventQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * 管理端：首页等统计接口。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final Set<String> PROCESSING_STATUSES = Set.of("PENDING", "SENT", "PARTIAL", "RETRYING");

    private final EventQueryService eventQueryService;
    private final TopicConfigRepository topicConfigRepository;
    private final TopicConsumerRepository topicConsumerRepository;

    public StatsController(
        EventQueryService eventQueryService,
        TopicConfigRepository topicConfigRepository,
        TopicConsumerRepository topicConsumerRepository
    ) {
        this.eventQueryService = eventQueryService;
        this.topicConfigRepository = topicConfigRepository;
        this.topicConsumerRepository = topicConsumerRepository;
    }

    /**
     * 首页统计：指定日期（或今日）的处理中事件数、当日事件总数、Topic 总数、消费者总数、0-23 时每小时事件数。
     * @param date 可选，yyyy-MM-dd，不传则使用当天（服务器默认时区）。
     */
    @GetMapping("/home")
    public HomeStatsDto home(
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date
    ) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate target = date != null ? date : LocalDate.now(zone);
        Instant dayStart = target.atStartOfDay(zone).toInstant();
        Instant dayEnd = target.plusDays(1).atStartOfDay(zone).toInstant();

        long todayProcessingCount = eventQueryService.countEventsWithStatusInAndOccurredAtBetween(
            PROCESSING_STATUSES, dayStart, dayEnd
        );
        long totalEventCount = eventQueryService.countEventsWithStatusInAndOccurredAtBetween(
            null, dayStart, dayEnd
        );
        long topicCount = topicConfigRepository.count();
        long consumerCount = topicConsumerRepository.count();
        List<Long> hourlyCounts = eventQueryService.countEventsByHourOfDay(
            dayStart, dayEnd, zone
        );

        return new HomeStatsDto(
            target.toString(),
            todayProcessingCount,
            totalEventCount,
            topicCount,
            consumerCount,
            hourlyCounts
        );
    }
}
