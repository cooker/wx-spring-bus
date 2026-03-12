## 项目提示词（给 AI / 新成员看的项目说明）

你现在在维护一个基于 **JDK 21 + Spring Boot 3.2** 的「事件总线 + 管理端」项目，项目名为 **wx-spring-bus**。  
整体目标：提供一个可复用的事件调度组件（bus），支持事件的记录、投递、消费反馈与状态汇总，并配套一个管理后台（man + man-vue）查看事件链路、消费状态并支持重推。

---

### 1. 整体结构与职责

- **根模块**
  - `bus/`：事件总线组件（Jar 形式，被业务应用/管理端引入）
  - `man/`：管理端后端（Spring Boot 应用，端口默认 8081）
  - `man-vue/`：管理端前端（Vue 3 + Vite）
  - `docs/`：需求、设计与使用文档

- **技术栈**
  - 运行时：JDK 21、Spring Boot 3.2.x
  - 存储：MongoDB（事件与消费记录）
  - 消息：RabbitMQ（事件投递与消费汇总）
  - 前端：Vue 3 + Vite + Ant Design Vue + ECharts
  - 风格：DDD（domains / application / infrastructure 分层）

---

### 2. bus 模块（事件总线组件）

> 典型用法：业务服务引入 `bus` 依赖，通过注解 `@PublishEvent` 或门面类发布事件，并通过消费端接口接入事件。

- **主要能力**
  - 发布事件：校验 topic、落库到 `events`、初始化 `event_consumptions`、发送 MQ、更新状态
  - 消费反馈：记录每次消费成功/失败，支持多次重试，写入 `event_consumptions`
  - 消费汇总：按 `(eventId, consumerId)` 聚合最新消费结果，异步回写 `events.status`
  - AOP 注解支持：`@PublishEvent` + `@EventInitiator`

- **关键包（bus/src/main/java/com/wx/bus）**
  - `annotation`：`@PublishEvent`、`@EventInitiator`
  - `aop`：`PublishEventAspect`，在方法成功返回后自动发布事件
  - `application`：业务用例服务，如 `EventPublishService`、`ConsumptionFeedbackService`、`ConsumptionRollupService`
  - `domain`：核心领域对象，如 `EventEnvelope`、`EventStatus`、`Initiator`
  - `infrastructure.mongo`：Mongo 仓储与文档对象（`EventDocument`、`EventConsumptionDocument`、`TopicConsumerDocument` 等）
  - `infrastructure.rabbit`：RabbitMQ 相关配置与 Publisher / Listener
  - `api`：对外门面与 DTO（`WxBusEventPublisher`，`WxBusConsumptionFeedback` 等）

- **数据表（Mongo 集合）**
  - `events`：一条业务事件一条文档，记录 payload、topic、状态、重试次数等
  - `event_consumptions`：消费反馈历史，按 `(eventId, consumerId, attemptNo)` 存多行
  - `topic_consumers`：配置某个 topic 对应的消费者列表

---

### 3. man 模块（管理端后端）

> 作用：对事件与消费记录进行检索、查询详情、查看链路，并提供重推能力；同时给前端提供统计接口和配置管理接口。

- **主要能力**
  - 事件列表 / 详情 / 链路视图查询：根据 status/topic/userId/时间范围/traceId 等过滤
  - 消费记录查看：按事件查看各消费者的历史消费记录
  - Topic 与消费者配置管理：CRUD `topic_configs` / `topic_consumers`
  - 重推事件：对失败或特定事件重新投递
  - 首页统计：今日/指定日期的事件数量、处理中数量、每小时分布、Topic / 消费者总数

- **关键包（man/src/main/java/com/wx/man）**
  - `api`：REST Controller（`EventController`、`StatsController`、`TopicConfigController`、`TopicConsumerController`）
  - `api.dto`：管理端使用的 DTO（事件列表/详情、消费记录、Topic/消费者配置、首页统计等）
  - `application`：`EventQueryService` 等查询/统计服务
  - `config`：`WebConfig`（CORS + 静态资源 + SPA 回退）、`SpaIndexResourceResolver`

- **静态资源**
  - `man` 通过 `classpath:/static/` 提供前端静态资源，并使用 SPA 回退到 `index.html`（支持 Vue Router history 模式）。

---

### 4. man-vue 模块（管理端前端）

> Vue 3 + Vite + Ant Design Vue 的 SPA 管理界面，对接 `man` 的 `/api/**` REST 接口。

- **主要页面**
  - 首页：展示今日/指定日期的事件总数、处理中数量、Topic/消费者数量、0–23 时事件数量柱状图
  - 事件列表：按条件筛选事件并分页展示
  - 事件详情 / 视图：查看事件 payload、状态变更时间线、消费记录表格等
  - Topic 配置页：管理 topic 元数据
  - Topic–消费者配置页：管理某个 topic 绑定的消费者列表、是否启用、排序等

- **关键文件**
  - `src/views/*.vue`：各业务页面
  - `src/router/index.js`：路由配置（使用 `createWebHistory`）
  - 通过 `/api` 前缀访问 `man`，开发时 Vite dev server 使用代理，生产时由 `man` 统一提供。

---

### 5. 运行与测试（本地 + Docker）

- **依赖服务（MongoDB + RabbitMQ）可用 Docker 快速启动**（见 `docs/docker.md`）：
  - RabbitMQ（管理端口 15672，账号 admin/123456）
  - MongoDB（root 用户 admin/123456）

- **典型命令**
  - 运行管理端（前后端分离开发）：
    ```bash
    # 后端
    mvn -pl man spring-boot:run
    # 前端
    cd man-vue && npm i && npm run dev
    ```
  - 运行测试：
    ```bash
    mvn -pl bus test
    mvn -pl man test
    ```

---

### 6. 设计原则 / 注意事项

1. **DDD 分层清晰**：domain 只放业务概念；application 负责编排流程；infrastructure 负责技术细节（Mongo、MQ 等）。  
2. **事件不可丢 & 可追踪**：所有事件必须先写入 `events`，再发 MQ；消费结果写入 `event_consumptions`，并通过 traceId/parentEventId 形成调用链。  
3. **消费反馈只写从表**：消费者侧只写 `event_consumptions`，不直接修改 `events`；状态汇总通过专用 rollup 流程异步回写。  
4. **多消费者支持**：同一事件的不同消费者通过 `topic_consumers` 配置，以 `(eventId, consumerId)` 维度管理消费状态。  
5. **管理端只读 + 重推**：`man` 默认不发业务事件，只负责管理、观测与在需要时触发重推。

---

### 7. 当你在这个项目里继续开发时，可以这么和 AI 说

>「这是一个基于 JDK21 + Spring Boot3 + MongoDB + RabbitMQ 的事件总线项目，`bus` 是可复用的事件调度组件，`man` + `man-vue` 是管理端。  
>请在保持 DDD 分层与现有 Mongo/Rabbit 设计的前提下，帮我在 XXX 场景下新增/修改 XXX 功能，并补充必要的测试与文档。」

你可以把上面这段提示给 AI 或新同事，让他们快速理解项目并按同样的风格继续开发。
