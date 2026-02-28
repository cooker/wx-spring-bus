package com.wx.bus.aop;

import com.wx.bus.annotation.EventInitiator;
import com.wx.bus.annotation.PublishEvent;
import com.wx.bus.application.EventPublishService;
import com.wx.bus.support.LogContext;
import com.wx.bus.application.PublishResult;
import com.wx.bus.domain.EventEnvelope;
import com.wx.bus.domain.Initiator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.time.Instant;
import java.util.Optional;

/**
 * 对带 {@link PublishEvent} 的方法在成功返回后发布事件；不抛异常，不影响主流程。
 * <p>由 {@link com.wx.bus.config.WxBusConfiguration} 注册为 Bean。</p>
 * <p>不作用于 Controller/RestController，避免与 Spring Data 的 ProjectingArgumentResolver 等 Web 参数解析冲突。</p>
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
public class PublishEventAspect {

    private static final Logger log = LoggerFactory.getLogger(PublishEventAspect.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final EventPublishService eventPublishService;

    public PublishEventAspect(EventPublishService eventPublishService) {
        this.eventPublishService = eventPublishService;
    }

    /** 无参 pointcut，且使用 @Around 而非 @AfterReturning(returning=)，避免 formal unbound in pointcut。 */
    @Pointcut(
        "@annotation(com.wx.bus.annotation.PublishEvent) "
            + "&& !@within(org.springframework.stereotype.Controller) "
            + "&& !@within(org.springframework.web.bind.annotation.RestController)"
    )
    public void publishEventMethod() {}

    @Around("publishEventMethod()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        PublishEvent ann = signature.getMethod().getAnnotation(PublishEvent.class);
        if (ann == null) return result;

        String topic = ann.topic();
        if (topic == null || topic.isBlank()) {
            log.warn("PublishEvent topic is blank, skip userId={}", LogContext.getUserId());
            return result;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("result", result);
        Object[] args = joinPoint.getArgs();
        String[] names = signature.getParameterNames();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                context.setVariable(names[i], i < args.length ? args[i] : null);
                context.setVariable("p" + i, i < args.length ? args[i] : null);
            }
        }

        Object payload = eval(ann.payload(), context, result);
        if (payload == null && "#result".equals(ann.payload())) {
            log.debug("PublishEvent payload(#result) is null, skip topic={} userId={}", topic, LogContext.getUserId());
            return result;
        }

        Instant occurAt = evalInstant(ann.occurAt(), context);
        String traceId = evalString(ann.traceId(), context);
        String spanId = evalString(ann.spanId(), context);
        String parentEventId = evalString(ann.parentEventId(), context);

        String initiatorService = ann.initiatorService();
        String initiatorOperation = ann.initiatorOperation();
        if ((initiatorService == null || initiatorService.isEmpty()) || (initiatorOperation == null || initiatorOperation.isEmpty())) {
            EventInitiator init = Optional.ofNullable(AnnotationUtils.findAnnotation(signature.getMethod(), EventInitiator.class))
                .orElse(AnnotationUtils.findAnnotation(signature.getDeclaringType(), EventInitiator.class));
            if (init != null) {
                if (initiatorService == null || initiatorService.isEmpty()) initiatorService = init.service();
                if (initiatorOperation == null || initiatorOperation.isEmpty()) initiatorOperation = init.operation();
            }
        }
        Initiator initiator = (initiatorService != null && !initiatorService.isEmpty()) || (initiatorOperation != null && !initiatorOperation.isEmpty())
            ? new Initiator(
                initiatorService != null ? initiatorService : "",
                initiatorOperation != null ? initiatorOperation : "",
                null,
                null
            )
            : null;

        EventEnvelope envelope = EventPublishService.buildEnvelope(
            null,
            traceId,
            spanId,
            parentEventId,
            topic,
            payload,
            EventEnvelope.PAYLOAD_TYPE_JSON,
            initiator,
            occurAt,
            null
        );

        try {
            PublishResult pr = eventPublishService.publish(envelope);
            if (!pr.success()) {
                log.error("PublishEvent failed topic={} eventId={} message={} userId={}", topic, pr.eventId(), pr.message(), LogContext.getUserId());
            }
        } catch (Exception e) {
            log.error("PublishEvent error topic={} userId={}", topic, LogContext.getUserId(), e);
        }
        return result;
    }

    private static Object eval(String expr, StandardEvaluationContext context, Object defaultVal) {
        if (expr == null || expr.isBlank()) return defaultVal;
        try {
            return PARSER.parseExpression(expr).getValue(context);
        } catch (Exception e) {
            log.debug("SpEL eval failed expr={} userId={}", expr, LogContext.getUserId(), e);
            return defaultVal;
        }
    }

    private static Instant evalInstant(String expr, StandardEvaluationContext context) {
        if (expr == null || expr.isBlank()) return Instant.now();
        try {
            Object v = PARSER.parseExpression(expr).getValue(context);
            if (v instanceof Instant i) return i;
            if (v != null) return Instant.parse(v.toString());
        } catch (Exception e) {
            log.debug("SpEL eval instant failed expr={} userId={}", expr, LogContext.getUserId(), e);
        }
        return Instant.now();
    }

    private static String evalString(String expr, StandardEvaluationContext context) {
        if (expr == null || expr.isBlank()) return null;
        try {
            Object v = PARSER.parseExpression(expr).getValue(context);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            log.debug("SpEL eval string failed expr={} userId={}", expr, LogContext.getUserId(), e);
            return null;
        }
    }
}
