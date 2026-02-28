package com.wx.bus.application.port;

import java.time.Instant;

/**
 * 消费反馈端口：上报一次消费结果，供监听器调用，便于测试 mock。
 */
public interface ConsumptionFeedbackPort {

    /**
     * 上报一次消费结果。
     */
    void recordFeedback(String eventId,
                        String consumerId,
                        boolean success,
                        Instant consumedAt,
                        String errorMessage,
                        String errorCode);
}
