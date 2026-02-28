package com.wx.bus.config;

import com.wx.bus.aop.PublishEventAspect;
import com.wx.bus.application.EventPublishService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Bus 组件配置：注册 {@link PublishEventAspect}，仅在存在 {@link EventPublishService} 时生效。
 * <p>bus 为组件，无独立启动类；由接入方在启动类上使用 {@link EnableWxBus} 引入。</p>
 * <p>使用默认 {@code proxyTargetClass = false}，且切面排除 Controller/RestController，避免与 Spring Data 的
 * ProjectingArgumentResolver（接口投影代理）冲突。</p>
 * <p>测试可设置 {@code bus.aop.enabled=false} 关闭切面，避免与测试上下文冲突。</p>
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnBean(EventPublishService.class)
@ConditionalOnProperty(name = "bus.aop.enabled", havingValue = "true", matchIfMissing = true)
public class WxBusConfiguration {

    @Bean
    public PublishEventAspect publishEventAspect(EventPublishService eventPublishService) {
        return new PublishEventAspect(eventPublishService);
    }
}
