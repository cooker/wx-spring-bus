package com.wx.bus.infrastructure.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * topic_consumers 集合文档（与设计文档 2.3 一致）。
 * <p>维护 topic 与消费者的关联，支持动态配置；仅 enabled=true 的配置参与发送校验与 event_consumptions 初始化。</p>
 */
@Document(collection = "topic_consumers")
@CompoundIndex(name = "topic_consumerId", def = "{'topic': 1, 'consumerId': 1}", unique = true)
public class TopicConsumerDocument {

    @Id
    private String id;
    private String topic;
    private String consumerId;
    private boolean enabled;
    private Integer sortOrder;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getConsumerId() { return consumerId; }
    public void setConsumerId(String consumerId) { this.consumerId = consumerId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
