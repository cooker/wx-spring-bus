package com.wx.bus.application;

import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.EventStatus;
import com.wx.bus.domain.Initiator;
import com.wx.bus.infrastructure.mongo.EventDocument;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * EventEnvelope 与 EventDocument 的转换；Initiator 与 Map 互转供 MongoDB 存储。
 */
public final class EventDocumentMapper {

    private EventDocumentMapper() {}

    /**
     * 从 events 文档恢复信封（供管理端重推等使用）。
     */
    public static EventEnvelope documentToEnvelope(EventDocument doc) {
        if (doc == null) return null;
        return new EventEnvelope(
            doc.getEventId(),
            doc.getTraceId(),
            doc.getSpanId(),
            doc.getParentEventId(),
            doc.getTopic(),
            doc.getPayload() != null ? doc.getPayload() : "",
            doc.getPayloadType() != null ? doc.getPayloadType() : EventEnvelope.PAYLOAD_TYPE_JSON,
            mapToInitiator(doc.getInitiator()),
            doc.getOccurredAt(),
            doc.getSentAt(),
            doc.getExpireAt()
        );
    }

    /**
     * 将信封与状态信息转为 events 集合文档（id = eventId）。
     */
    static EventDocument toDocument(EventEnvelope envelope, EventStatus status, Instant statusAt,
                                     int retryCount, Instant lastSentAt, Instant createdAt, Instant updatedAt) {
        EventDocument doc = new EventDocument();
        doc.setId(envelope.eventId());
        doc.setEventId(envelope.eventId());
        doc.setTraceId(envelope.traceId());
        doc.setSpanId(envelope.spanId());
        doc.setParentEventId(envelope.parentEventId());
        doc.setTopic(envelope.topic());
        doc.setPayload(envelope.payload());
        doc.setPayloadType(envelope.payloadType());
        doc.setInitiator(initiatorToMap(envelope.initiator()));
        doc.setOccurredAt(envelope.occurredAt());
        doc.setSentAt(envelope.sentAt());
        doc.setExpireAt(envelope.expireAt());
        doc.setStatus(status.name());
        doc.setStatusAt(statusAt);
        doc.setRetryCount(retryCount);
        doc.setLastSentAt(lastSentAt);
        doc.setCreatedAt(createdAt);
        doc.setUpdatedAt(updatedAt);
        return doc;
    }

    /** Initiator 转为 Map，便于存入 EventDocument.initiator */
    static Map<String, String> initiatorToMap(Initiator initiator) {
        if (initiator == null) return Collections.emptyMap();
        return Map.of(
            "service", Optional.ofNullable(initiator.service()).orElse(""),
            "operation", Optional.ofNullable(initiator.operation()).orElse(""),
            "userId", Optional.ofNullable(initiator.userId()).orElse(""),
            "clientRequestId", Optional.ofNullable(initiator.clientRequestId()).orElse("")
        );
    }

    /** 从 EventDocument.initiator Map 恢复 Initiator */
    static Initiator mapToInitiator(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        return new Initiator(
            map.getOrDefault("service", ""),
            map.getOrDefault("operation", ""),
            map.getOrDefault("userId", ""),
            map.getOrDefault("clientRequestId", "")
        );
    }
}
