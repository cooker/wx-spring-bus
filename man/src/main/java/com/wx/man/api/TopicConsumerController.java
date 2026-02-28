package com.wx.man.api;

import com.wx.bus.infrastructure.mongo.TopicConsumerDocument;
import com.wx.bus.infrastructure.mongo.TopicConsumerRepository;
import com.wx.man.api.dto.TopicConsumerDto;
import com.wx.man.api.dto.TopicConsumerRequestDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * 管理端：Topic–消费者配置表 CRUD。
 */
@RestController
@RequestMapping("/api/topic-consumers")
public class TopicConsumerController {

    private final TopicConsumerRepository topicConsumerRepository;

    public TopicConsumerController(TopicConsumerRepository topicConsumerRepository) {
        this.topicConsumerRepository = topicConsumerRepository;
    }

    /**
     * 全量列表；可选按 topic、consumerId 筛选。
     */
    @GetMapping
    public List<TopicConsumerDto> list(
        @RequestParam(required = false) String topic,
        @RequestParam(required = false) String consumerId
    ) {
        Sort sort = Sort.by("topic", "sortOrder", "consumerId");
        String t = topic != null && !topic.isBlank() ? topic.trim() : null;
        String c = consumerId != null && !consumerId.isBlank() ? consumerId.trim() : null;
        List<TopicConsumerDocument> list;
        if (t != null && c != null) {
            list = topicConsumerRepository.findByTopicAndConsumerId(t, c, sort);
        } else if (t != null) {
            list = topicConsumerRepository.findByTopic(t, sort);
        } else if (c != null) {
            list = topicConsumerRepository.findByConsumerId(c, sort);
        } else {
            list = topicConsumerRepository.findAll(sort);
        }
        return list.stream().map(this::toDto).toList();
    }

    /**
     * 单条详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<TopicConsumerDto> get(@PathVariable String id) {
        return topicConsumerRepository.findById(id)
            .map(doc -> ResponseEntity.ok(toDto(doc)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 新增。
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TopicConsumerRequestDto body) {
        String topic = body.topic().trim();
        String consumerId = body.consumerId().trim();
        if (topicConsumerRepository.findOneByTopicAndConsumerId(topic, consumerId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageBody(false, "同一 topic 下已存在该 consumerId"));
        }
        Instant now = Instant.now();
        TopicConsumerDocument doc = new TopicConsumerDocument();
        doc.setTopic(topic);
        doc.setConsumerId(consumerId);
        doc.setEnabled(body.enabled());
        doc.setSortOrder(body.sortOrder());
        doc.setDescription(body.description() != null ? body.description().trim() : null);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc = topicConsumerRepository.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(doc));
    }

    /**
     * 更新。
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody TopicConsumerRequestDto body) {
        Optional<TopicConsumerDocument> opt = topicConsumerRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TopicConsumerDocument doc = opt.get();
        String newTopic = body.topic().trim();
        String newConsumerId = body.consumerId().trim();
        Optional<TopicConsumerDocument> existing = topicConsumerRepository.findOneByTopicAndConsumerId(newTopic, newConsumerId);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageBody(false, "同一 topic 下已存在该 consumerId"));
        }
        doc.setTopic(newTopic);
        doc.setConsumerId(newConsumerId);
        doc.setEnabled(body.enabled());
        doc.setSortOrder(body.sortOrder());
        doc.setDescription(body.description() != null ? body.description().trim() : null);
        doc.setUpdatedAt(Instant.now());
        doc = topicConsumerRepository.save(doc);
        return ResponseEntity.ok(toDto(doc));
    }

    /**
     * 删除。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!topicConsumerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        topicConsumerRepository.deleteById(id);
        return ResponseEntity.ok().body(new MessageBody(true, "已删除"));
    }

    private TopicConsumerDto toDto(TopicConsumerDocument doc) {
        return new TopicConsumerDto(
            doc.getId(),
            doc.getTopic(),
            doc.getConsumerId(),
            doc.isEnabled(),
            doc.getSortOrder(),
            doc.getDescription(),
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }

    private record MessageBody(boolean success, String message) {}
}
