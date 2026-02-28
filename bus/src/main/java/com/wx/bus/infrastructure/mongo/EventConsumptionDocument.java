package com.wx.bus.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * event_consumptions 集合文档（与设计文档 2.5 一致）。
 * <p>无 (eventId, consumerId) 唯一约束；同一消费者多次回调各插入一条（attemptNo 递增），便于排查。</p>
 */
@Document(collection = "event_consumptions")
public class EventConsumptionDocument {

    @Id
    private String id;
    @Indexed
    private String eventId;
    private String consumerId;
    /** 0=初始化，1/2/3…=第 1/2/3 次回调 */
    private int attemptNo;
    /** null=待消费（初始化记录），true/false=该次消费结果 */
    private Boolean success;
    private Instant consumedAt;
    private String errorMessage;
    private String errorCode;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getConsumerId() { return consumerId; }
    public void setConsumerId(String consumerId) { this.consumerId = consumerId; }
    public int getAttemptNo() { return attemptNo; }
    public void setAttemptNo(int attemptNo) { this.attemptNo = attemptNo; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
