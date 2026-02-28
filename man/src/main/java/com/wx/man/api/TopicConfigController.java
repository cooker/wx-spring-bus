package com.wx.man.api;

import com.wx.bus.infrastructure.mongo.TopicConfigDocument;
import com.wx.bus.infrastructure.mongo.TopicConfigRepository;
import com.wx.man.api.dto.TopicConfigDto;
import com.wx.man.api.dto.TopicConfigRequestDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import java.util.List;
import java.util.Optional;

/**
 * 管理端：Topic 配置表 CRUD（维护 topic 中文名称等）。
 */
@RestController
@RequestMapping("/api/topic-configs")
public class TopicConfigController {

    private final TopicConfigRepository topicConfigRepository;

    public TopicConfigController(TopicConfigRepository topicConfigRepository) {
        this.topicConfigRepository = topicConfigRepository;
    }

    /**
     * 全量列表；可选按 topic 关键字筛选。
     */
    @GetMapping
    public List<TopicConfigDto> list(@RequestParam(required = false) String topic) {
        Sort sort = Sort.by("topic");
        List<TopicConfigDocument> list = topic != null && !topic.isBlank()
            ? topicConfigRepository.findAllByTopicContaining(topic.trim(), sort)
            : topicConfigRepository.findAll(sort);
        return list.stream().map(this::toDto).toList();
    }

    /**
     * 单条详情（按 id）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<TopicConfigDto> get(@PathVariable String id) {
        return topicConfigRepository.findById(id)
            .map(doc -> ResponseEntity.ok(toDto(doc)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 新增。
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TopicConfigRequestDto body) {
        String topic = body.topic().trim();
        if (topicConfigRepository.findByTopic(topic).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageBody(false, "该 topic 已存在"));
        }
        Instant now = Instant.now();
        TopicConfigDocument doc = new TopicConfigDocument();
        doc.setTopic(topic);
        doc.setNameZh(body.nameZh() != null ? body.nameZh().trim() : null);
        doc.setDescription(body.description() != null ? body.description().trim() : null);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc = topicConfigRepository.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(doc));
    }

    /**
     * 更新。
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody TopicConfigRequestDto body) {
        Optional<TopicConfigDocument> opt = topicConfigRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String newTopic = body.topic().trim();
        TopicConfigDocument doc = opt.get();
        if (!doc.getTopic().equals(newTopic) && topicConfigRepository.findByTopic(newTopic).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageBody(false, "该 topic 已存在"));
        }
        doc.setTopic(newTopic);
        doc.setNameZh(body.nameZh() != null ? body.nameZh().trim() : null);
        doc.setDescription(body.description() != null ? body.description().trim() : null);
        doc.setUpdatedAt(Instant.now());
        doc = topicConfigRepository.save(doc);
        return ResponseEntity.ok(toDto(doc));
    }

    /**
     * 删除。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!topicConfigRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        topicConfigRepository.deleteById(id);
        return ResponseEntity.ok().body(new MessageBody(true, "已删除"));
    }

    private TopicConfigDto toDto(TopicConfigDocument doc) {
        return new TopicConfigDto(
            doc.getId(),
            doc.getTopic(),
            doc.getNameZh(),
            doc.getDescription(),
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }

    private record MessageBody(boolean success, String message) {}
}
