# Java 开发者手册

本文档面向参与 **wx-spring-bus** 后端开发的 Java 工程师，说明环境、项目结构、编码规范、DDD 分层、事件与存储约定、配置与测试等，便于快速上手与统一实现风格。

---

## 1. 文档与项目索引

| 文档 | 说明 |
|------|------|
| [需求文档](./requirements.md) | 项目概述、功能需求、模块职责 |
| [事件与存储设计](./event-and-storage-design.md) | 事件结构、MongoDB 集合、发送/消费/重推/汇总流程 |
| 本文档 | Java 开发环境、规范、分层、实现要点 |

**代码仓库**：根目录为多模块 Maven 项目，`bus` 与 `man` 为两个可独立运行的 Spring Boot 应用。

---

## 2. 开发环境

### 2.1 必需

| 项目 | 版本/要求 |
|------|-----------|
| **JDK** | 21（LTS），项目统一使用 21 |
| **Maven** | 3.8+，用于构建 |
| **MongoDB** | 本地或远程，默认库名 `wx-bus` |
| **RabbitMQ** | 本地或远程，默认 vhost `/` |

### 2.2 推荐

- **IDE**：IntelliJ IDEA 或 Eclipse，需支持 JDK 21 与 Maven 多模块。
- **编码**：UTF-8；换行符与项目一致（通常 LF）。

### 2.3 快速校验

```bash
java -version   # 应显示 21.x
mvn -v          # 3.8+
# MongoDB、RabbitMQ 需可连接（见 application.yml）
```

---

## 3. 项目结构

### 3.1 模块划分

```
wx-spring-bus/
├── pom.xml              # 父 POM，JDK 21、Spring Boot 3.2.x
├── bus/                 # 事件总线组件：发布、消费、状态与反馈（无启动类）
│   ├── pom.xml
│   └── src/main/java/com/wx/bus/
│       ├── domain/      # 领域层
│       ├── application/ # 应用服务层
│       └── infrastructure/ # 基础设施（MongoDB、RabbitMQ、汇总消费者等）
└── man/                 # 管理端：查询、重推、配置
    ├── pom.xml
    └── src/main/java/com/wx/man/
        ├── WxManApplication.java
        ├── api/         # REST 控制器
        ├── domain/
        ├── application/
        └── infrastructure/
```

- **bus**：**组件**，无独立启动类、无端口；负责事件发送、消费反馈写入、消费汇总队列消费者（异步回写 events.status）；由 man 或业务应用引入依赖并启用 `@EnableWxBus`。
- **man**：端口默认 8081；负责 events/event_consumptions/topic_consumers 的查询与重推、topic_consumers 配置维护（若由本模块提供接口）。

### 3.2 DDD 包职责（bus / man 通用）

| 包 | 职责 | 说明 |
|----|------|------|
| **domain** | 领域模型、值对象、领域服务、枚举 | 不依赖 Spring、不直接访问 DB/MQ |
| **application** | 应用服务（用例） | 编排 domain 与 infrastructure，事务边界通常在此 |
| **infrastructure** | 持久化、MQ、外部调用 | MongoDB Repository、RabbitMQ 发布/监听、消费汇总消费者 |
| **api**（仅 man） | REST 控制器、DTO 入参/出参 | 薄层，校验入参后委托 application |

依赖方向：**api → application → domain**；**application → infrastructure**；**infrastructure** 实现 domain 或 application 定义的接口（若采用接口抽象）。

---

## 4. 技术栈与依赖

### 4.1 父 POM

- **Java**：21  
- **Spring Boot**：3.2.x（由 parent 管理）  
- **打包**：jar

### 4.2 bus

- `spring-boot-starter-web`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-amqp`
- `spring-boot-starter-test`（test）

### 4.3 man

- `spring-boot-starter-web`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-amqp`
- `spring-boot-starter-validation`
- `spring-boot-starter-test`（test）

---

## 5. 编码规范

### 5.1 命名

- **包名**：小写，`com.wx.bus.domain`、`com.wx.man.api` 等。
- **类名**：大驼峰；接口可不加 `I` 前缀，如 `EventPublisher`。
- **方法/变量**：小驼峰；常量全大写下划线。
- **DTO/Request/Response**：见 5.2。

### 5.2 DTO 与 API

- **请求体**：`XxxRequest`，可用 **record** 定义，配合 `@Valid` 与 Bean Validation。
- **响应体**：`XxxResponse` 或 `XxxDto`，优先 **record**。
- **内部传输**：如应用层与基础设施层之间传递的 DTO，可命名为 `XxxDto` 或 record。

示例：

```java
// 请求
public record PublishEventRequest(
    String topic,
    Object payload,
    String traceId,
    String parentEventId,
    InitiatorDto initiator
) {}

// 响应
public record PublishEventResponse(String eventId, String status) {}
```

### 5.3 日志

- 使用 **SLF4J**（`LoggerFactory`）或 **Lombok `@Slf4j`**，不直接使用 `System.out` 或 log4j 等实现类。
- **Bus 内所有日志**均包含 **userId**：从 SLF4J **MDC** 的 `userId` 键读取；未设置时输出空字符串。接入方在调用 bus 前（如请求入口、异步任务入口）应设置 `MDC.put("userId", userId)`，便于全链路排查。
- **无 topic_consumers 配置时**：打 **ERROR** 日志（含 topic、eventId、userId 等），**不抛异常**，终止发送并返回，不影响主流程（与 [事件与存储设计](./event-and-storage-design.md) 3.2 一致）。
- 关键流程（发送、消费、重推、汇总失败）建议打 INFO 或 WARN，便于排查。

### 5.4 异常

- 业务校验失败：可抛自定义业务异常或使用 `IllegalArgumentException` 等，由 api 层统一映射为 HTTP 状态码与错误体。
- **发送流程中「无 topic_consumers 配置」**：不抛异常，仅 ERROR 日志 + 终止发送。

### 5.5 空安全

- 推荐使用 **Optional** 或显式 null 检查；若引入 **Jakarta Bean Validation**，在 Request 上使用 `@NotNull` 等注解。

---

## 6. 事件与存储（实现要点）

以下与 [事件与存储设计](./event-and-storage-design.md) 一致，实现时请以该文档为准。

### 6.1 事件信封（Envelope）

- 字段：eventId、traceId、spanId、parentEventId、topic、payload、payloadType、initiator、occurredAt、sentAt、expireAt。
- **initiator**：service、operation、userId、clientRequestId。
- 与 MongoDB **events** 文档及 RabbitMQ 消息体保持一致结构（如 JSON 序列化）。

### 6.2 MongoDB 集合

- **events**：事件主表；仅存 envelope + 发送/重试相关状态（PENDING/SENT/RETRYING 等）；消费态（CONSUMED/PARTIAL/FAILED）由异步回写写入。
- **topic_consumers**：topic–消费者配置；(topic, consumerId) 唯一；enabled=true 的配置参与发送前校验与初始化 event_consumptions。
- **event_consumptions**：消费反馈；**无** (eventId, consumerId) 唯一约束；每次回调**插入**新行（attemptNo 递增）；汇总时按 (eventId, consumerId) 取最新一条。

### 6.3 发送流程（先落库后发 MQ）

1. 生成/校验 envelope。  
2. **校验 topic_consumers**：按 topic 查 enabled=true；若无任何配置，**打 ERROR 日志、不抛异常、终止发送**。  
3. 写 **events**（status=PENDING）+ 按配置初始化 **event_consumptions**（attemptNo=0, success=null）。  
4. 发布到 RabbitMQ。  
5. 更新 events 为 SENT、sentAt、lastSentAt。

### 6.4 消费反馈与汇总

- 消费端（或 bus 代写）**仅写 event_consumptions**：每次回调**插入**一条（eventId、consumerId、attemptNo 递增、success、consumedAt、errorMessage）。
- **消费汇总**：采用**异步回写**；写入 event_consumptions 后向**消费汇总专用 topic** 投递 eventId，由**单一消费者**串行：按 eventId 查 event_consumptions → 按 (eventId, consumerId) 取最新一条 → 汇总 status → 更新 events.status/statusAt。专用 topic 与业务 topic 区分，命名可约定如 `bus.consumption-rollup`。

### 6.5 重推

- man 按 eventId 查 events，调用 bus 或直接发 MQ 再次发布；更新 events 的 retryCount、lastSentAt、status。消费端按 eventId（及 consumerId）做幂等。

---

## 7. 配置要点

### 7.1 bus（application.yml）

- `spring.application.name`: bus  
- `server.port`: 8080  
- `spring.data.mongodb.uri`: 指向 wx-bus 库  
- `spring.rabbitmq.*`: host、port、username、password、virtual-host  
- 业务事件 exchange/queue 与**消费汇总专用 topic/队列**需在配置或代码中声明并绑定。

### 7.2 man（application.yml）

- `spring.application.name`: man  
- `server.port`: 8081  
- `spring.data.mongodb.uri`: 与 bus 同库（或只读权限）  
- `spring.rabbitmq.*`: 用于重推发布（若由 man 直接发）或与 bus 共用

### 7.3 环境隔离

- 可通过 `spring.profiles.active` 切换 `application-{profile}.yml`，区分 dev/test/prod 的 MongoDB、RabbitMQ 地址与端口。

---

## 8. 测试

### 8.1 单元测试

- **JUnit 5** + **Mockito**；测试类包路径与源码对应，类名 `XxxTest`。
- 对 application 层用例做单测时，可 mock infrastructure（Repository、RabbitTemplate 等）。
- domain 层纯逻辑尽量无依赖，便于单测。

### 8.2 集成测试

- 使用 `@SpringBootTest`，按需启用 `@DataMongoDBTest`、`@AutoConfigureMockMvc` 或真实 MongoDB/RabbitMQ（如 testcontainers）。
- 集成测试可放在各模块的 `src/test` 下，命名如 `XxxIntegrationTest`。

### 8.3 运行测试

```bash
mvn test
mvn -pl bus test
mvn -pl man test
```

- **bus 单元测试**（`EventPublishServiceTest`）使用本机 **MongoDB**、**RabbitMQ**，通过 `application-test.yml` 配置测试库（如 `wx-bus-test`）与连接；运行前需本机已启动二者。

---

## 9. 常用命令

```bash
# 编译
mvn compile -pl bus,man

# 打包
mvn package -pl bus,man -DskipTests

# 运行 man（bus 为组件，无独立启动）
mvn -pl man spring-boot:run

# 清理
mvn clean
```

---

## 10. 扩展与注意事项

- **新增 topic**：在 **topic_consumers** 中为该 topic 配置至少一条 enabled=true 的 consumerId，否则发送时会打 ERROR 并终止发送（不抛异常）。  
- **消费端**：按 eventId（及 consumerId）做幂等；反馈时只写 event_consumptions 并投递 eventId 到消费汇总 topic，不直接改 events。  
- **JDK 21**：可按需使用虚拟线程、Record、Pattern Matching 等特性，与现有 Spring Boot 3.x 兼容。  
- **依赖升级**：版本以父 POM 与 Spring BOM 为准，新增依赖时注意与现有 Spring Boot 版本兼容。

---

**文档版本**：v0.1  
**维护**：与 [需求文档](./requirements.md)、[事件与存储设计](./event-and-storage-design.md) 同步更新；实现有歧义时以设计文档为准。
