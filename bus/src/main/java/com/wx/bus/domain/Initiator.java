package com.wx.bus.domain;

/**
 * 事件发起方（与设计文档 1.2 initiator 一致）。
 *
 * @param service         发起服务名，如 order-service、bus
 * @param operation       操作/接口，如 createOrder、POST /api/orders
 * @param userId          若可识别则填用户 ID
 * @param clientRequestId 客户端请求 ID，便于与业务日志关联
 */
public record Initiator(
    String service,
    String operation,
    String userId,
    String clientRequestId
) {}
