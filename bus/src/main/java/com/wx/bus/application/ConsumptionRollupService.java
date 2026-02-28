package com.wx.bus.application;

import com.wx.bus.domain.EventStatus;
import com.wx.bus.support.LogContext;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消费汇总：按 (eventId, consumerId) 取最新一条（attemptNo 最大），汇总为 CONSUMED/PARTIAL/FAILED 后回写 events.status。
 * <p>由消费汇总专用队列的 {@link com.wx.bus.infrastructure.rabbit.RollupListener} 串行调用。</p>
 */
@Service
public class ConsumptionRollupService {

    private static final Logger log = LoggerFactory.getLogger(ConsumptionRollupService.class);

    private final EventRepository eventRepository;
    private final EventConsumptionRepository eventConsumptionRepository;

    public ConsumptionRollupService(
        EventRepository eventRepository,
        EventConsumptionRepository eventConsumptionRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventConsumptionRepository = eventConsumptionRepository;
    }

    /**
     * 根据 event_consumptions 汇总该事件的消费状态并写回 events 文档。
     * <p>规则：存在 success=null → SENT；全失败 → FAILED；全成功 → CONSUMED；否则 PARTIAL。</p>
     */
    public void rollupAndWriteBack(String eventId) {
        EventDocument event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            log.warn("Event not found for rollup eventId={} userId={}", eventId, LogContext.getUserId());
            return;
        }

        List<EventConsumptionDocument> all = eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(eventId);
        Map<String, EventConsumptionDocument> latestByConsumer = all.stream()
            .collect(Collectors.toMap(EventConsumptionDocument::getConsumerId, c -> c, (a, b) -> a));

        long withNull = latestByConsumer.values().stream().filter(c -> c.getSuccess() == null).count();
        long successCount = latestByConsumer.values().stream().filter(c -> Boolean.TRUE.equals(c.getSuccess())).count();
        long failCount = latestByConsumer.values().stream().filter(c -> Boolean.FALSE.equals(c.getSuccess())).count();
        int total = latestByConsumer.size();

        String newStatus;
        if (withNull > 0) {
            newStatus = EventStatus.SENT.name();
        } else if (failCount == total) {
            newStatus = EventStatus.FAILED.name();
        } else if (successCount == total) {
            newStatus = EventStatus.CONSUMED.name();
        } else {
            newStatus = EventStatus.PARTIAL.name();
        }

        Instant now = Instant.now();
        event.setStatus(newStatus);
        event.setStatusAt(now);
        event.setUpdatedAt(now);
        eventRepository.save(event);
        log.debug("Rollup eventId={} status={} userId={}", eventId, newStatus, LogContext.getUserId());
    }
}
