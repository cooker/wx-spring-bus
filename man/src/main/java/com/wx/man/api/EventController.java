package com.wx.man.api;

import com.wx.bus.application.EventRetryService;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.man.api.dto.EventConsumptionItemDto;
import com.wx.man.api.dto.EventDetailDto;
import com.wx.man.api.dto.EventListItemDto;
import com.wx.man.application.EventQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 管理端：事件列表、详情、重推。
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final EventConsumptionRepository eventConsumptionRepository;
    private final EventRetryService eventRetryService;
    private final EventQueryService eventQueryService;

    public EventController(
        EventRepository eventRepository,
        EventConsumptionRepository eventConsumptionRepository,
        EventRetryService eventRetryService,
        EventQueryService eventQueryService
    ) {
        this.eventRepository = eventRepository;
        this.eventConsumptionRepository = eventConsumptionRepository;
        this.eventRetryService = eventRetryService;
        this.eventQueryService = eventQueryService;
    }

    /**
     * 分页列表；可选按 status、topic、userId、occurredAt 范围筛选。
     * occurredAtFrom/occurredAtTo 为 ISO-8601 格式（如 2025-02-28T00:00:00Z）。
     */
    @GetMapping
    public Page<EventListItemDto> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String topic,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String occurredAtFrom,
        @RequestParam(required = false) String occurredAtTo,
        @RequestParam(required = false) String traceId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Instant from = parseInstant(occurredAtFrom);
        Instant to = parseInstant(occurredAtTo);
        Pageable pageable = PageRequest.of(page, size);
        Page<EventDocument> docPage = eventQueryService.findEvents(status, topic, userId, from, to, traceId, pageable);
        return docPage.map(this::toListItem);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 事件详情（含消费记录）。
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailDto> detail(@PathVariable String eventId) {
        return eventRepository.findById(eventId)
            .map(doc -> {
                List<EventConsumptionDocument> consumptions =
                    eventConsumptionRepository.findByEventIdOrderByAttemptNoDesc(eventId);
                return ResponseEntity.ok(toDetail(doc, consumptions));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 重推：再次发布到 MQ 并更新 retryCount/status。
     */
    @PostMapping("/{eventId}/retry")
    public ResponseEntity<Object> retry(@PathVariable String eventId) {
        boolean ok = eventRetryService.retry(eventId);
        if (ok) {
            return ResponseEntity.ok().body(new RetryResult(true, "重推已提交"));
        }
        return ResponseEntity.badRequest().body(new RetryResult(false, "事件不存在或状态不允许重推"));
    }

    private EventListItemDto toListItem(EventDocument doc) {
        return new EventListItemDto(
            doc.getEventId(),
            doc.getParentEventId(),
            doc.getTopic(),
            doc.getStatus(),
            doc.getOccurredAt(),
            doc.getStatusAt(),
            doc.getRetryCount(),
            doc.getInitiator()
        );
    }

    private EventDetailDto toDetail(EventDocument doc, List<EventConsumptionDocument> consumptions) {
        List<EventConsumptionItemDto> list = consumptions.stream()
            .map(c -> new EventConsumptionItemDto(
                c.getId(),
                c.getConsumerId(),
                c.getAttemptNo(),
                c.getSuccess(),
                c.getConsumedAt(),
                c.getErrorMessage(),
                c.getErrorCode(),
                c.getCreatedAt()
            ))
            .toList();
        return new EventDetailDto(
            doc.getEventId(),
            doc.getTraceId(),
            doc.getSpanId(),
            doc.getParentEventId(),
            doc.getTopic(),
            doc.getPayload(),
            doc.getPayloadType(),
            doc.getInitiator(),
            doc.getOccurredAt(),
            doc.getSentAt(),
            doc.getExpireAt(),
            doc.getStatus(),
            doc.getStatusAt(),
            doc.getRetryCount(),
            doc.getLastSentAt(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            list
        );
    }

    private record RetryResult(boolean success, String message) {}
}
