package com.wx.bus.application;

import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.EventStatus;
import com.wx.bus.application.port.EventPublisherPort;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 事件重推：根据 eventId 从 events 加载信封，再次发布到 MQ 并更新 retryCount/status。
 * <p>供管理端 man 调用；仅允许 FAILED、RETRYING、SENT 状态重推。</p>
 */
@Service
public class EventRetryService {

    private static final Logger log = LoggerFactory.getLogger(EventRetryService.class);

    private final EventRepository eventRepository;
    private final EventPublisherPort eventPublisher;

    public EventRetryService(EventRepository eventRepository, EventPublisherPort eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 重推指定事件：发布到 MQ 并更新 events 的 retryCount、lastSentAt、status。
     *
     * @param eventId 事件 ID
     * @return true 重推成功，false 事件不存在或状态不允许重推
     */
    public boolean retry(String eventId) {
        EventDocument doc = eventRepository.findById(eventId).orElse(null);
        if (doc == null) {
            log.warn("Retry skipped: event not found eventId={} userId={}", eventId, LogContext.getUserId());
            return false;
        }
        String status = doc.getStatus();
        if (status == null || !isRetryableStatus(status)) {
            log.warn("Retry skipped: status not retryable eventId={} status={} userId={}", eventId, status, LogContext.getUserId());
            return false;
        }
        if (doc.getExpireAt() != null && doc.getExpireAt().isBefore(Instant.now())) {
            log.warn("Retry skipped: event expired eventId={} userId={}", eventId, LogContext.getUserId());
            return false;
        }
        EventEnvelope envelope = EventDocumentMapper.documentToEnvelope(doc);
        try {
            eventPublisher.publish(envelope);
        } catch (Exception e) {
            log.error("Retry publish failed eventId={} userId={}", eventId, LogContext.getUserId(), e);
            return false;
        }
        Instant now = Instant.now();
        doc.setRetryCount(doc.getRetryCount() + 1);
        doc.setLastSentAt(now);
        doc.setStatus(EventStatus.SENT.name());
        doc.setStatusAt(now);
        doc.setUpdatedAt(now);
        eventRepository.save(doc);
        log.info("Retry done eventId={} retryCount={} userId={}", eventId, doc.getRetryCount(), LogContext.getUserId());
        return true;
    }

    private static boolean isRetryableStatus(String status) {
        return EventStatus.FAILED.name().equals(status)
            || EventStatus.RETRYING.name().equals(status)
            || EventStatus.SENT.name().equals(status);
    }
}
