package com.wx.bus.api.dto;

import com.wx.bus.domain.Initiator;

/**
 * 事件发起方信息 DTO，与领域对象 {@link Initiator} 一一对应。
 * <p>
 * 主要用于在对外接口 / JSON 中携带「是谁在什么操作下触发了该事件」的信息，
 * 便于在管理端或日志中按服务/操作/用户维度筛查事件。
 * </p>
 *
 * @param service         发起方服务名，如 {@code order-service}
 * @param operation       发起方操作名，如 {@code createOrder}
 * @param userId          触发事件的业务用户 ID，可为空
 * @param clientRequestId 客户端请求 ID，可用于前后端／多系统之间相关联请求排查，可为空
 */
public record InitiatorDto(String service, String operation, String userId, String clientRequestId) {

    /**
     * 转为领域对象 {@link Initiator}。
     */
    public Initiator toInitiator() {
        return new Initiator(service, operation, userId, clientRequestId);
    }

    /**
     * 从领域对象构建 DTO；入参为空时返回 {@code null}。
     */
    public static InitiatorDto from(Initiator i) {
        if (i == null) return null;
        return new InitiatorDto(i.service(), i.operation(), i.userId(), i.clientRequestId());
    }
}
