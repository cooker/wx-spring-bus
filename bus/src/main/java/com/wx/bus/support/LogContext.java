package com.wx.bus.support;

import org.slf4j.MDC;

/**
 * 日志上下文：从 MDC 读取当前请求/调用链的 userId，供所有日志打印统一包含。
 * <p>接入方在调用 bus 前设置 {@code MDC.put("userId", userId)}，bus 内所有日志将带上 userId。</p>
 */
public final class LogContext {

    public static final String USER_ID_KEY = "userId";

    private LogContext() {}

    /**
     * 当前线程 MDC 中的 userId，未设置时返回空字符串。
     */
    public static String getUserId() {
        String u = MDC.get(USER_ID_KEY);
        return u != null ? u : "";
    }
}
