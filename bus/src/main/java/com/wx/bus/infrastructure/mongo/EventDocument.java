package com.wx.bus.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * events 集合文档（与设计文档 2.2 一致）。
 * <p>仅存 envelope + 发送/重试相关状态；消费态（CONSUMED/PARTIAL/FAILED）由消费汇总异步回写。</p>
 */
@Document(collection = "events")
public class EventDocument {

    /** 使用 eventId 作为 _id，便于幂等与 findById(eventId) */
    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;
    private String traceId;
    private String spanId;
    private String parentEventId;
    @Indexed
    private String topic;
    private Object payload;
    private String payloadType;
    /** 发起方：service, operation, userId, clientRequestId */
    private Map<String, String> initiator;
    private Instant occurredAt;
    private Instant sentAt;
    private Instant expireAt;
    @Indexed
    private String status;
    private Instant statusAt;
    private int retryCount;
    private Instant lastSentAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    public String getParentEventId() { return parentEventId; }
    public void setParentEventId(String parentEventId) { this.parentEventId = parentEventId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }
    public Map<String, String> getInitiator() { return initiator; }
    public void setInitiator(Map<String, String> initiator) { this.initiator = initiator; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStatusAt() { return statusAt; }
    public void setStatusAt(Instant statusAt) { this.statusAt = statusAt; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
