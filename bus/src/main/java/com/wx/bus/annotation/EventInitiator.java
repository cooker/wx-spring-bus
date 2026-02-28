package com.wx.bus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明事件发起方，用于 {@link PublishEvent} 未指定 initiatorService/initiatorOperation 时。
 * <p>可标在类或方法上，方法优先于类。</p>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventInitiator {

    String service() default "";

    String operation() default "";
}
