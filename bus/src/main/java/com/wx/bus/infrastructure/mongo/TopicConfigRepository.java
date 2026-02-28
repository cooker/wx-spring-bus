package com.wx.bus.infrastructure.mongo;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * topic 配置表仓储。
 */
public interface TopicConfigRepository extends MongoRepository<TopicConfigDocument, String> {

    Optional<TopicConfigDocument> findByTopic(String topic);

    List<TopicConfigDocument> findAllByTopicContaining(String topic, Sort sort);
}
