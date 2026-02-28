package com.wx.bus.application;

import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.EventStatus;
import com.wx.bus.domain.Initiator;
import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.mongo.EventDocument;
import com.wx.bus.infrastructure.mongo.EventRepository;
import com.wx.bus.infrastructure.mongo.TopicConsumerDocument;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;
import com.wx.bus.application.port.EventPublisherPort;
import com.wx.bus.support.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 事件发送（先落库后发 MQ）。
 * <p>流程：校验 topic_consumers → 写 events(PENDING) + 初始化 event_consumptions → 发 MQ → 更新 SENT。</p>
 * <p>无 topic_consumers 配置时打 ERROR 日志、返回 {@link PublishResult#skipped}，不抛异常、不写库不发 MQ。</p>
 */
@Service
public class EventPublishService {

    private static final Logger log = LoggerFactory.getLogger(EventPublishService.class);

    private final TopicConsumerRepository topicConsumerRepository;
    private final EventRepository eventRepository;
    private final EventConsumptionRepository eventConsumptionRepository;
    private final EventPublisherPort eventPublisher;

    public EventPublishService(
        TopicConsumerRepository topicConsumerRepository,
        EventRepository eventRepository,
        EventConsumptionRepository eventConsumptionRepository,
        EventPublisherPort eventPublisher
    ) {
        this.topicConsumerRepository = topicConsumerRepository;
        this.eventRepository = eventRepository;
        this.eventConsumptionRepository = eventConsumptionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布事件。无 topic_consumers 配置时记录 ERROR、返回 PublishResult(success=false)，不抛异常。
     */
    @Transactional
    public PublishResult publish(EventEnvelope envelope) {
        String eventId = envelope.eventId();
        String topic = envelope.topic();

        List<TopicConsumerDocument> consumers = topicConsumerRepository.findByTopicAndEnabledTrue(topic);
        if (consumers == null || consumers.isEmpty()) {
            log.error("No enabled topic_consumers for topic={}, eventId={}; aborting send userId={}", topic, eventId, LogContext.getUserId());
            return PublishResult.skipped(eventId, "No enabled consumers for topic: " + topic);
        }

        Instant now = Instant.now();
        EventDocument doc = EventDocumentMapper.toDocument(
            envelope, EventStatus.PENDING, now, 0, null, now, now
        );
        doc.setId(eventId);
        eventRepository.save(doc);

        for (TopicConsumerDocument tc : consumers) {
            EventConsumptionDocument consumption = new EventConsumptionDocument();
            consumption.setEventId(eventId);
            consumption.setConsumerId(tc.getConsumerId());
            consumption.setAttemptNo(0);
            consumption.setSuccess(null);
            consumption.setConsumedAt(null);
            consumption.setCreatedAt(now);
            eventConsumptionRepository.save(consumption);
        }

        EventEnvelope toPublish = new EventEnvelope(
            envelope.eventId(), envelope.traceId(), envelope.spanId(), envelope.parentEventId(),
            envelope.topic(), envelope.payload(), envelope.payloadType(), envelope.initiator(),
            envelope.occurredAt(), now, envelope.expireAt()
        );
        try {
             eventPublisher.publish(toPublish);
        } catch (Exception e) {
            log.error("Failed to publish eventId={} to MQ userId={}", eventId, LogContext.getUserId(), e);
            doc.setStatus(EventStatus.FAILED.name());
            doc.setStatusAt(Instant.now());
            doc.setUpdatedAt(Instant.now());
            eventRepository.save(doc);
            return PublishResult.skipped(eventId, "MQ publish failed: " + e.getMessage());
        }

        doc.setStatus(EventStatus.SENT.name());
        doc.setStatusAt(now);
        doc.setSentAt(now);
        doc.setLastSentAt(now);
        doc.setUpdatedAt(now);
        eventRepository.save(doc);

        return PublishResult.ok(eventId);
    }

    /**
     * 构建信封：若未提供 eventId 则生成 UUID；occurredAt 未提供则用当前时间。
     */
    public static EventEnvelope buildEnvelope(
        String eventId,
        String traceId,
        String spanId,
        String parentEventId,
        String topic,
        Object payload,
        String payloadType,
        Initiator initiator,
        Instant occurredAt,
        Instant expireAt
    ) {
        String id = eventId != null && !eventId.isBlank() ? eventId : UUID.randomUUID().toString();
        Instant occ = occurredAt != null ? occurredAt : Instant.now();
        return new EventEnvelope(
            id, traceId, spanId, parentEventId, topic,
            payload, payloadType != null ? payloadType : EventEnvelope.PAYLOAD_TYPE_JSON,
            initiator, occ, null, expireAt
        );
    }
}
