package com.wx.bus.application;

import com.wx.bus.infrastructure.mongo.EventConsumptionDocument;
import com.wx.bus.support.LogContext;
import com.wx.bus.infrastructure.mongo.EventConsumptionRepository;
import com.wx.bus.infrastructure.rabbit.RollupPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 消费反馈：每次回调插入一条 event_consumptions（attemptNo 递增），并投递 eventId 到消费汇总队列触发异步回写。
 * <p>不覆盖已有记录，便于保留多次重试历史。</p>
 */
@Service
public class ConsumptionFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(ConsumptionFeedbackService.class);

    private final EventConsumptionRepository eventConsumptionRepository;
    private final RollupPublisher rollupPublisher;

    public ConsumptionFeedbackService(
        EventConsumptionRepository eventConsumptionRepository,
        RollupPublisher rollupPublisher
    ) {
        this.eventConsumptionRepository = eventConsumptionRepository;
        this.rollupPublisher = rollupPublisher;
    }

    /**
     * 记录一次消费反馈：插入新行（attemptNo = 该 consumer 当前最大+1），并发送 eventId 到汇总队列。
     */
    public void recordFeedback(String eventId, String consumerId, boolean success,
                               Instant consumedAt, String errorMessage, String errorCode) {
        int nextAttempt = nextAttemptNo(eventId, consumerId);
        EventConsumptionDocument doc = new EventConsumptionDocument();
        doc.setEventId(eventId);
        doc.setConsumerId(consumerId);
        doc.setAttemptNo(nextAttempt);
        doc.setSuccess(success);
        doc.setConsumedAt(consumedAt != null ? consumedAt : Instant.now());
        doc.setErrorMessage(errorMessage);
        doc.setErrorCode(errorCode);
        doc.setCreatedAt(Instant.now());
        eventConsumptionRepository.save(doc);
        rollupPublisher.sendEventIdForRollup(eventId);
        log.debug("Recorded consumption eventId={} consumerId={} attemptNo={} success={} userId={}", eventId, consumerId, nextAttempt, success, LogContext.getUserId());
    }

    /** 计算该 (eventId, consumerId) 下一条记录的 attemptNo */
    private int nextAttemptNo(String eventId, String consumerId) {
        List<EventConsumptionDocument> list = eventConsumptionRepository.findByEventIdAndConsumerIdOrderByAttemptNoDesc(eventId, consumerId);
        int max = list.isEmpty() ? 0 : list.get(0).getAttemptNo();
        return max + 1;
    }
}
