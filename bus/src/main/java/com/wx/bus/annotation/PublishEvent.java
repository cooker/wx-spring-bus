package com.wx.bus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明方法在成功返回后发布一条事件（AOP 拦截）。
 * <p>适用于业务方法执行成功后自动发事件，便于项目移植：仅加注解即可，无需手写 publish 代码。</p>
 * <ul>
 *   <li>{@link #topic()} 必填，事件 topic</li>
 *   <li>{@link #payload()} SpEL，默认 "#result" 表示方法返回值作为 payload</li>
 *   <li>initiator 可由本注解的 service/operation 或类/方法上的 {@link EventInitiator} 提供</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublishEvent {

    /**
     * 事件 topic，必填。
     */
    String topic();

    /**
     * 载荷 SpEL，默认方法返回值。例："#result"、"#p0"、"{'orderId':#p0.id}"。
     */
    String payload() default "#result";

    /**
     * 发生时间 SpEL，默认当前时间。例："T(java.time.Instant).now()"。
     */
    String occurAt() default "T(java.time.Instant).now()";

    /**
     * traceId SpEL，可选。例："#p0.traceId"、""。
     */
    String traceId() default "";

    /**
     * spanId SpEL，可选。
     */
    String spanId() default "";

    /**
     * parentEventId SpEL，可选。
     */
    String parentEventId() default "";

    /**
     * 发起方 service，与 {@link EventInitiator} 二选一或互补（本注解优先）。
     */
    String initiatorService() default "";

    /**
     * 发起方 operation。
     */
    String initiatorOperation() default "";
}
