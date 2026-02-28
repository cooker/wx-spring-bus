package com.wx.man.application;

import com.wx.bus.infrastructure.mongo.EventDocument;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 事件列表动态查询：支持 status、topic、userId、occurredAt 范围。
 */
@Service
public class EventQueryService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "occurredAt");

    private final MongoTemplate mongoTemplate;

    public EventQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 分页查询；任一参数为 null 或空则不参与筛选。
     *
     * @param occurredAtFrom occurredAt 起始（>=），ISO-8601 或毫秒时间戳
     * @param occurredAtTo   occurredAt 截止（<=）
     * @param traceId        按链路 ID 筛选（事件视图用）
     */
    public Page<EventDocument> findEvents(
        String status,
        String topic,
        String userId,
        Instant occurredAtFrom,
        Instant occurredAtTo,
        String traceId,
        Pageable pageable
    ) {
        Query q = new Query();
        if (status != null && !status.isBlank()) {
            q.addCriteria(Criteria.where("status").is(status.trim()));
        }
        if (topic != null && !topic.isBlank()) {
            q.addCriteria(Criteria.where("topic").is(topic.trim()));
        }
        if (userId != null && !userId.isBlank()) {
            q.addCriteria(Criteria.where("initiator.userId").is(userId.trim()));
        }
        if (occurredAtFrom != null) {
            q.addCriteria(Criteria.where("occurredAt").gte(occurredAtFrom));
        }
        if (occurredAtTo != null) {
            q.addCriteria(Criteria.where("occurredAt").lte(occurredAtTo));
        }
        if (traceId != null && !traceId.isBlank()) {
            q.addCriteria(Criteria.where("traceId").is(traceId.trim()));
        }
        long total = mongoTemplate.count(q, EventDocument.class);
        q.with(DEFAULT_SORT).with(pageable);
        List<EventDocument> content = mongoTemplate.find(q, EventDocument.class);
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 统计 status 在给定集合内且 occurredAt 在 [from, to] 范围内的事件数量（用于首页「今日处理中」等）。
     */
    public long countEventsWithStatusInAndOccurredAtBetween(
        Set<String> statuses,
        Instant occurredAtFrom,
        Instant occurredAtTo
    ) {
        Query q = new Query();
        if (statuses != null && !statuses.isEmpty()) {
            q.addCriteria(Criteria.where("status").in(statuses));
        }
        if (occurredAtFrom != null || occurredAtTo != null) {
            Criteria range = Criteria.where("occurredAt");
            if (occurredAtFrom != null) range.gte(occurredAtFrom);
            if (occurredAtTo != null) range.lte(occurredAtTo);
            q.addCriteria(range);
        }
        return mongoTemplate.count(q, EventDocument.class);
    }

    /**
     * 按小时（0-23）统计某时间范围内的事件数量。小时以服务器默认时区为准。
     * 返回列表长度为 24，索引为小时，值为该小时的事件数（未出现的小时为 0）。
     */
    public List<Long> countEventsByHourOfDay(Instant fromInclusive, Instant toExclusive, ZoneId zone) {
        MatchOperation match = Aggregation.match(
            Criteria.where("occurredAt").gte(fromInclusive).lt(toExclusive)
        );
        Document dateToString = new Document("$dateToString",
            new Document("date", "$occurredAt")
                .append("format", "%H")
                .append("timezone", zone.getId()));
        Document hourExpr = new Document("$toInt", new Document("$substr", List.of(dateToString, 0, 2)));
        Document projectStage = new Document("$project", new Document("hour", hourExpr));
        AggregationOperation projectHour = new AggregationOperation() {
            @Override
            public Document toDocument(AggregationOperationContext context) {
                return projectStage;
            }
        };
        GroupOperation group = Aggregation.group("hour").count().as("count");
        Aggregation agg = Aggregation.newAggregation(match, projectHour, group);
        AggregationResults<Document> results = mongoTemplate.aggregate(
            agg, EventDocument.class, Document.class
        );
        List<Long> hourly = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            hourly.add(0L);
        }
        for (Document doc : results.getMappedResults()) {
            int hour = doc.getInteger("_id", 0);
            if (hour >= 0 && hour < 24) {
                Object c = doc.get("count");
                hourly.set(hour, c instanceof Number n ? n.longValue() : 0L);
            }
        }
        return hourly;
    }
}
