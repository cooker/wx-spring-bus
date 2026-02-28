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
 * 消费反馈：若该 (eventId, consumerId) 最新记录为待消费（success=null）则更新该条，否则插入新行（attemptNo 递增），并投递 eventId 到汇总队列。
 * <p>首次回调更新初始化记录，后续重试/再次回调则新增行，便于保留多次重试历史。</p>
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
     * 记录一次消费反馈：若最新记录为待消费（success=null）则更新该条，否则插入新行（attemptNo 递增），并发送 eventId 到汇总队列。
     */
    public void recordFeedback(String eventId, String consumerId, boolean success,
                               Instant consumedAt, String errorMessage, String errorCode) {
        Instant at = consumedAt != null ? consumedAt : Instant.now();
        List<EventConsumptionDocument> list = eventConsumptionRepository.findByEventIdAndConsumerIdOrderByAttemptNoDesc(eventId, consumerId);
        EventConsumptionDocument latest = list.isEmpty() ? null : list.get(0);

        if (latest != null && latest.getSuccess() == null) {
            // 待消费：更新该条，不新增
            latest.setSuccess(success);
            latest.setConsumedAt(at);
            latest.setErrorMessage(errorMessage);
            latest.setErrorCode(errorCode);
            eventConsumptionRepository.save(latest);
            rollupPublisher.sendEventIdForRollup(eventId);
            log.debug("Updated consumption (pending) eventId={} consumerId={} attemptNo={} success={} userId={}", eventId, consumerId, latest.getAttemptNo(), success, LogContext.getUserId());
            return;
        }

        // 已有明确结果或尚无记录：新增一行
        int nextAttempt = nextAttemptNo(list);
        EventConsumptionDocument doc = new EventConsumptionDocument();
        doc.setEventId(eventId);
        doc.setConsumerId(consumerId);
        doc.setAttemptNo(nextAttempt);
        doc.setSuccess(success);
        doc.setConsumedAt(at);
        doc.setErrorMessage(errorMessage);
        doc.setErrorCode(errorCode);
        doc.setCreatedAt(Instant.now());
        eventConsumptionRepository.save(doc);
        rollupPublisher.sendEventIdForRollup(eventId);
        log.debug("Recorded consumption eventId={} consumerId={} attemptNo={} success={} userId={}", eventId, consumerId, nextAttempt, success, LogContext.getUserId());
    }

    /** 根据已有列表计算下一条 attemptNo（列表已按 attemptNo 降序） */
    private int nextAttemptNo(List<EventConsumptionDocument> list) {
        int max = list.isEmpty() ? 0 : list.get(0).getAttemptNo();
        return max + 1;
    }
}
