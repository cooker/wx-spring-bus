package com.wx.bus.event;

import com.wx.bus.application.ConsumptionFeedbackService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听 {@link ConsumptionFeedbackEvent}，调用 {@link ConsumptionFeedbackService#recordFeedback}。
 */
@Component
public class ConsumptionFeedbackListener {

    private final ConsumptionFeedbackService consumptionFeedbackService;

    public ConsumptionFeedbackListener(ConsumptionFeedbackService consumptionFeedbackService) {
        this.consumptionFeedbackService = consumptionFeedbackService;
    }

    @EventListener
    public void onConsumptionFeedback(ConsumptionFeedbackEvent event) {
        consumptionFeedbackService.recordFeedback(
            event.getEventId(),
            event.getConsumerId(),
            event.isSuccess(),
            event.getConsumedAt(),
            event.getErrorMessage(),
            event.getErrorCode()
        );
    }
}
