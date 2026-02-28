package com.wx.bus.infrastructure.mongo;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * topic_consumers 仓储；发送前按 topic 查启用中的消费者配置；管理端全量列表。
 */
public interface TopicConsumerRepository extends MongoRepository<TopicConsumerDocument, String> {

    /**
     * 按 topic 查询所有启用中的消费者配置，用于发送校验与初始化 event_consumptions。
     */
    List<TopicConsumerDocument> findByTopicAndEnabledTrue(String topic);

    /**
     * 按 topic 查询并排序（管理端列表）。
     */
    List<TopicConsumerDocument> findByTopic(String topic, Sort sort);

    /**
     * 按 consumerId 查询并排序（管理端列表）。
     */
    List<TopicConsumerDocument> findByConsumerId(String consumerId, Sort sort);

    /**
     * 按 topic + consumerId 查询并排序（管理端列表）。
     */
    List<TopicConsumerDocument> findByTopicAndConsumerId(String topic, String consumerId, Sort sort);

    /**
     * 按 topic + consumerId 查一条（唯一性校验）。
     */
    Optional<TopicConsumerDocument> findOneByTopicAndConsumerId(String topic, String consumerId);
}
