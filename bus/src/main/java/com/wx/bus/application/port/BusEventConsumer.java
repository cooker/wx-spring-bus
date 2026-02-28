package com.wx.bus.application.port;

import com.wx.bus.domain.EventEnvelope;

/**
 * 业务事件消费者接口：接入方实现此接口并注册为 Bean，bus 从 MQ 拉取事件后回调。
 * <p>方法正常返回视为消费成功，抛出异常视为失败；bus 会据此写入消费反馈并触发汇总。</p>
 */
@FunctionalInterface
public interface BusEventConsumer {

    /**
     * 处理一条事件。实现方按 topic 做业务逻辑；异常将导致记录为消费失败并上报反馈。
     *
     * @param envelope 事件信封（含 eventId、topic、payload 等）
     */
    void onEvent(EventEnvelope envelope);
}
