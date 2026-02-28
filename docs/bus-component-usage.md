# Bus 组件使用说明（AOP + 注解 / 公共类 + Spring 事件）

Bus **不提供 REST 接口**，通过以下方式接入：
- **公共类**：`WxBusEventPublisher`、`WxBusConsumptionFeedback`，内部通过 Spring 事件触发发布与消费反馈。
- **AOP + 注解**：`@PublishEvent`，方法成功返回后自动发布事件。

---

## 1. 公共类（Spring 事件）

### 1.1 事件发布

注入 **WxBusEventPublisher**，调用 `publish(EventEnvelope)` 或 `publish(PublishEventRequest)`。内部发布 **EventPublishRequestEvent**，由 **EventPublishListener** 同步执行落库与发 MQ，并回写 **PublishResult**。

```java
@Autowired
private WxBusEventPublisher wxBusEventPublisher;

// 方式一：信封
EventEnvelope envelope = EventPublishService.buildEnvelope(...);
PublishResult result = wxBusEventPublisher.publish(envelope);

// 方式二：请求体（DTO）
PublishResult result = wxBusEventPublisher.publish(request);
```

### 1.2 消费者（拉取事件并上报反馈）

配置 `bus.consumer.consumer-id` 与 `bus.consumer.topics`（逗号分隔），并实现 **BusEventConsumer** 接口、注册为 Bean。bus 会声明队列 `bus.consumer.{consumer-id}` 并绑定到业务事件交换机，拉取到消息后反序列化为 **EventEnvelope**、调用 `onEvent(envelope)`，并根据是否抛异常自动上报消费反馈（成功/失败）。

```yaml
bus:
  consumer:
    consumer-id: member-service
    topics: order.purchased,order.cancelled
```

```java
@Component
public class MemberServiceEventConsumer implements BusEventConsumer {

    @Override
    public void onEvent(EventEnvelope envelope) {
        if ("order.purchased".equals(envelope.topic())) {
            // 按 payload 做业务处理，如加积分
        }
    }
}
```

消费成功：方法正常返回，bus 自动调用 **WxBusConsumptionFeedback**.recordFeedback(..., true)。消费失败：方法抛异常，bus 记录 false 并上报 errorMessage，然后重新抛出以触发 MQ 重试/DLQ。消费端应按 eventId 做幂等。

### 1.3 消费反馈（手动上报）

注入 **WxBusConsumptionFeedback**，调用 `recordFeedback(eventId, consumerId, success, consumedAt, errorMessage, errorCode)`。内部发布 **ConsumptionFeedbackEvent**，由 **ConsumptionFeedbackListener** 写入 event_consumptions 并触发汇总回写。

```java
@Autowired
private WxBusConsumptionFeedback wxBusConsumptionFeedback;

wxBusConsumptionFeedback.recordFeedback(eventId, "member-service", true, Instant.now(), null, null);
```

---

## 2. 注解说明（AOP）

### 2.1 @PublishEvent

在**方法**上使用，方法**成功返回后**发布一条事件（先落库后发 MQ）。失败或未配置 topic_consumers 时仅打日志，不抛异常，不影响主流程。

| 属性 | 类型 | 默认 | 说明 |
|------|------|------|------|
| **topic** | String | — | 必填，事件 topic |
| **payload** | String | `"#result"` | SpEL，默认方法返回值作为 payload |
| **occurAt** | String | 当前时间 | SpEL，事件发生时间 |
| **traceId** | String | "" | SpEL，可选 |
| **spanId** | String | "" | SpEL，可选 |
| **parentEventId** | String | "" | SpEL，可选 |
| **initiatorService** | String | "" | 发起方 service，可与 @EventInitiator 互补 |
| **initiatorOperation** | String | "" | 发起方 operation |

**SpEL 上下文**：方法参数名为变量（如 `order`）、`p0`/`p1`…、`result`（返回值）。

### 2.2 @EventInitiator

在**类**或**方法**上使用，为 @PublishEvent 提供发起方；若 @PublishEvent 已填 initiatorService/initiatorOperation 则以注解为准。方法上的优先于类上的。

---

## 3. 本仓库内使用（bus 模块）

已启用 `@EnableWxBus`，直接在任何 Spring Bean 的方法上使用即可：

```java
@Service
@EventInitiator(service = "order-service", operation = "createOrder")
public class OrderService {

    @PublishEvent(topic = "order.purchased")
    public OrderResult createOrder(CreateOrderCommand cmd) {
        // ... 业务逻辑
        return new OrderResult(orderId, amount);
    }
}
```

返回值会作为 payload；若要用入参作 payload：

```java
@PublishEvent(topic = "order.purchased", payload = "#p0")
public void createOrder(CreateOrderCommand cmd) { ... }
```

---

## 4. 移植到其他项目

### 4.1 依赖

其他 Spring Boot 项目引入 bus 模块（或后续的 bus-starter 构件）：

```xml
<dependency>
    <groupId>com.wx</groupId>
    <artifactId>bus</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 4.2 扫描与配置

- **方式一（推荐）**：扫描 bus 包并配置 Mongo/Rabbit，由自动配置注册 AOP。

```java
@SpringBootApplication(scanBasePackages = {"your.package", "com.wx.bus"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

在 `application.yml` 中配置 MongoDB、RabbitMQ（或与 bus 的 `application.yml` 约定一致）。

- **方式二**：显式启用组件。

```java
@SpringBootApplication(scanBasePackages = {"your.package", "com.wx.bus"})
@EnableWxBus
public class YourApplication { ... }
```

### 4.3 使用注解或公共类

在任意 Spring Bean 方法上使用 `@PublishEvent`、按需使用 `@EventInitiator`；或注入 **WxBusEventPublisher** / **WxBusConsumptionFeedback** 调用发布与消费反馈。需消费事件时配置 `bus.consumer.*` 并实现 **BusEventConsumer**。无 REST，无需 HTTP 调用。

### 4.4 关闭 AOP 发布

若只使用 bus 的 REST（发布/消费反馈）而不需要 AOP 发布，可排除自动配置：

```yaml
spring:
  autoconfigure:
    exclude:
      - com.wx.bus.config.WxBusConfiguration
```

或在启动类上：

```java
@SpringBootApplication(exclude = WxBusConfiguration.class)
```

---

## 5. 行为约定

- **执行时机**：方法正常返回后（@AfterReturning），再发布事件；方法抛异常则不发布。
- **不影响主流程**：发布失败（如无 topic_consumers、MQ 异常）只打 ERROR 日志，不抛异常。
- **先落库后发 MQ**：与设计文档一致；无 topic_consumers 时打 ERROR 并终止发送，不写库不发 MQ。

---

## 6. 参考

- 事件与存储设计：`docs/event-and-storage-design.md`
- Java 开发者手册：`docs/java-developer-guide.md`
