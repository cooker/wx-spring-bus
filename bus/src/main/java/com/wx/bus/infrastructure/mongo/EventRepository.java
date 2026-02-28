package com.wx.bus.infrastructure.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * events 集合；插入时以 eventId 作为 _id，便于幂等与 findById(eventId)。
 */
public interface EventRepository extends MongoRepository<EventDocument, String> {

    Page<EventDocument> findAllByOrderByOccurredAtDesc(Pageable pageable);

    Page<EventDocument> findByStatusOrderByOccurredAtDesc(String status, Pageable pageable);

    Page<EventDocument> findByTopicOrderByOccurredAtDesc(String topic, Pageable pageable);

    Page<EventDocument> findByStatusAndTopicOrderByOccurredAtDesc(String status, String topic, Pageable pageable);
}
