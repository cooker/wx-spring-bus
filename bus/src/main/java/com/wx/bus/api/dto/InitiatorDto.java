package com.wx.bus.api.dto;

import com.wx.bus.domain.Initiator;

/**
 * 发起方 DTO，与 {@link Initiator} 一一对应。
 */
public record InitiatorDto(String service, String operation, String userId, String clientRequestId) {

    public Initiator toInitiator() {
        return new Initiator(service, operation, userId, clientRequestId);
    }

    public static InitiatorDto from(Initiator i) {
        if (i == null) return null;
        return new InitiatorDto(i.service(), i.operation(), i.userId(), i.clientRequestId());
    }
}
