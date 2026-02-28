package com.wx.bus.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 WxBus 组件（AOP 发布事件）。
 * <p>在 Spring Boot 启动类或任意 @Configuration 上添加此注解，并确保扫描到 {@code com.wx.bus} 以加载事件发布服务与基础设施。</p>
 * <p>移植示例：</p>
 * <pre>
 * &#64;SpringBootApplication(scanBasePackages = {"your.package", "com.wx.bus"})
 * &#64;EnableWxBus
 * public class YourApplication { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(WxBusConfiguration.class)
public @interface EnableWxBus {
}
