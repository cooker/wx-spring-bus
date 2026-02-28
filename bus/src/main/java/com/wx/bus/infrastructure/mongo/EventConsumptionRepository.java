package com.wx.bus.infrastructure.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * event_consumptions 仓储；汇总时按 eventId 或 (eventId, consumerId) 取最新一条（attemptNo 最大）。
 */
public interface EventConsumptionRepository extends MongoRepository<EventConsumptionDocument, String> {

    /** 某事件下所有消费记录，按 attemptNo 降序（取最新用第一条） */
    List<EventConsumptionDocument> findByEventIdOrderByAttemptNoDesc(String eventId);

    /** 某事件某消费者的所有记录，按 attemptNo 降序，用于计算下一 attemptNo */
    List<EventConsumptionDocument> findByEventIdAndConsumerIdOrderByAttemptNoDesc(String eventId, String consumerId);
}
