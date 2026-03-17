# API 包结构与 DTO 审查报告（对照 Allra API 设计标准）

本文档对照 [Allra 后端 API 设计及包结构规则](.cursor/skills/allra-fintech-allra-api-design/SKILL.md)，对当前 **man** 与 **bus** 的 API 层、DTO 命名与 REST 约定进行审查，并给出可选的改进建议。

---

## 一、当前结构概览

### 1. man 模块（管理端后端）

```
com.wx.man
├── api
│   ├── EventController.java          @RequestMapping("/api/events")
│   ├── StatsController.java          @RequestMapping("/api/stats")
│   ├── TopicConfigController.java    @RequestMapping("/api/topic-configs")
│   ├── TopicConsumerController.java  @RequestMapping("/api/topic-consumers")
│   └── dto
│       ├── EventListItemDto.java
│       ├── EventDetailDto.java
│       ├── EventConsumptionItemDto.java
│       ├── TopicConfigDto.java
│       ├── TopicConfigRequestDto.java
│       ├── TopicConsumerDto.java
│       ├── TopicConsumerRequestDto.java
│       └── HomeStatsDto.java
├── application
│   └── EventQueryService.java
├── config
└── (domain / infrastructure 为 package-info 等)
```

### 2. bus 模块（事件总线，无 REST）

```
com.wx.bus.api
├── WxBusEventPublisher.java
├── WxBusConsumptionFeedback.java
└── dto
    ├── PublishEventRequest.java
    ├── PublishEventResponse.java
    ├── ConsumptionFeedbackRequest.java
    └── InitiatorDto.java
```

---

## 二、对照 Allra 标准的逐项审查

### 2.1 包结构（层分离）

| Allra 建议 | 当前 man | 结论与建议 |
|------------|----------|------------|
| 按**领域**分包：{domain}/api、{domain}/dto、{domain}/service… | 顶层按**层级**分包：api、application、config，无按 event/topic/stats 再分子包 | **可选**：若后续领域增多，可考虑 `api/event/`、`api/topic/`、`api/stats/` 等；当前规模下保持「api + application」分层即可。 |
| DTO 按用途分子包：`dto/request`、`dto/response` | 所有 DTO 在 `api/dto/` 平铺 | **建议**：增加 `api/dto/request` 与 `api/dto/response`，将请求体与响应体分类放置，便于维护和规范约束。 |

### 2.2 DTO 命名

| Allra 规则 | 当前命名 | 结论与建议 |
|------------|----------|------------|
| **Request**：`{Operation}Request`（如 CreateTopicConfigRequest） | `TopicConfigRequestDto`、`TopicConsumerRequestDto` | **建议**：改为「操作语义 + Request」。例如：`CreateTopicConfigRequest` / `UpdateTopicConfigRequest`（若创建与更新字段一致可保留单一 `TopicConfigRequest`）；TopicConsumer 同理。 |
| **Response/列表·详情**：`{Operation}Response` 或 `{Resource}DetailResponse` | `EventListItemDto`、`EventDetailDto`、`TopicConfigDto`、`HomeStatsDto` 等 | **建议**：与客户端契约的响应体统一为 `XxxResponse` 或 `XxxDetailResponse`。例如：`EventListItemResponse`、`EventDetailResponse`、`TopicConfigResponse`、`HomeStatsResponse`。保留 `Dto` 后缀给**仅内部**使用的 DTO（如 Service 间、与 Document 映射用）。 |
| **内部用 DTO**：`XxxDto` 后缀 | `EventConsumptionItemDto`、`InitiatorDto`（bus） | **符合**：内部/嵌套结构用 `Dto` 合理；若上述改为 Response 后，与 Allra 的「内部 DTO 用 Dto」一致。 |

### 2.3 REST 约定

| Allra 示例 | 当前实现 | 结论与建议 |
|------------|----------|------------|
| `GET /api/v1/resources` 列表、`GET /api/v1/resources/{id}` 单笔 | `GET /api/events`、`GET /api/events/{eventId}`；topic-configs、topic-consumers 同理 | **符合**。**可选**：如需版本管理可加 `/api/v1/` 前缀。 |
| `POST` 创建、`PUT` 全量更新、`PATCH` 部分更新、`DELETE` 删除 | TopicConfig / TopicConsumer 已用 POST/PUT/DELETE；Event 为 `POST /api/events/{eventId}/retry` | **符合**。retry 作为「动作」用 POST 子资源合理。 |
| 列表返回 `List<XxxResponse>`，单笔返回 `XxxDetailResponse` 或 404 | 列表返回 `Page<EventListItemDto>` 或 `List<TopicConfigDto>`；单笔 `ResponseEntity<EventDetailDto>` 等 | **符合**。若 DTO 改名为 Response，即与 Allra 命名一致。 |

### 2.4 Request 校验与 Record

| 项目 | 当前 | 结论 |
|------|------|------|
| Request 使用 Bean Validation | `TopicConfigRequestDto`、`TopicConsumerRequestDto` 等已用 `@NotBlank`、`@Size`、`@Valid` | **符合**。 |
| DTO 使用 record | 所有 DTO 均为 record | **符合**。 |

### 2.5 响应体统一封装（Allra 示例）

| Allra 示例 | 当前 | 建议 |
|------------|------|------|
| 成功：`{ "data": {...}, "message": "..." }`；错误：`{ "error": { "code", "message", "details" } }` | 直接返回 DTO/Page，错误用 `ResponseEntity.notFound()`、`body(MessageBody)` 等 | **可选**：若希望与 Allra 完全一致，可引入统一响应包装（如 `ApiResponse<T>`）和全局异常处理返回统一 error 结构；当前直返 DTO 更简单，适合内部管理端。 |

---

## 三、改进建议汇总（按优先级）

### 高优先级（建议做）

1. **DTO 命名与 Allra 对齐**
   - **Request**：`TopicConfigRequestDto` → 按操作拆成 `CreateTopicConfigRequest`、`UpdateTopicConfigRequest`（或保留一个 `TopicConfigRequest` 若创建/更新同体）。`TopicConsumerRequestDto` 同理。
   - **Response**：对外响应的 DTO 建议统一改为 `XxxResponse` / `XxxDetailResponse`：
     - `EventListItemDto` → `EventListItemResponse`
     - `EventDetailDto` → `EventDetailResponse`
     - `EventConsumptionItemDto` → 作为 `EventDetailResponse` 内嵌结构可保留 `EventConsumptionItemDto` 或改为 `EventConsumptionItemResponse`
     - `TopicConfigDto` → `TopicConfigResponse`
     - `TopicConsumerDto` → `TopicConsumerResponse`
     - `HomeStatsDto` → `HomeStatsResponse`
   - 仅内部使用的结构（如与 Mongo Document 映射、Service 间传递）保留 `Dto` 后缀。

2. **DTO 子包分离**
   - 在 `man` 下增加 `api/dto/request`、`api/dto/response`（必要时可保留 `api/dto` 放内部共用 DTO）。
   - 将请求体放入 `request`，响应体放入 `response`，便于规范与静态检查。

### 中优先级（可选）

3. **API 版本前缀**
   - 若未来有兼容性需求，可统一为 `@RequestMapping("/api/v1/events")` 等，便于后续 v2 共存。

4. **统一成功/错误响应格式**
   - 若希望与 Allra 文档完全一致，可增加 `ApiResponse<T>` 与 `ApiError`，并用 `@ControllerAdvice` 统一返回错误结构；对当前管理端非必须。

### 低优先级（视规模再定）

5. **按领域分子包**
   - 若 man 后续增加大量领域（如告警、审计、权限），再考虑 `api/event/`、`api/topic/`、`api/stats/` 等按领域分包；当前规模可维持现状。

---

## 四、bus 模块简要说明

- bus 无 REST，仅门面 + 事件；`PublishEventRequest` / `PublishEventResponse`、`ConsumptionFeedbackRequest` 已符合「Operation + Request/Response」命名。
- `InitiatorDto` 为内部/跨层传输用，保留 `Dto` 合理。
- 若 bus 未来提供 REST（如内部管理或调试接口），建议同样遵循 `{Operation}Request` / `{Operation}Response` 及 request/response 子包。

---

## 五、Checklist（按 Allra 自检）

- [x] 控制器在 api 层，业务逻辑在 application/service 层
- [x] Request DTO 使用 Bean Validation，DTO 使用 record
- [x] REST 使用 GET/POST/PUT/DELETE，路径与 Allra 示例一致
- [x] **Request 命名**：建议统一为 `{Operation}Request`（当前为 `XxxRequestDto`）
- [x] **Response 命名**：建议对外响应统一为 `XxxResponse`（当前部分为 `XxxDto`）
- [x] **DTO 子包**：建议增加 `dto/request`、`dto/response`
- [x] **API 版本**：按需增加 `/api/v1/` 与统一响应包装

---

**总结**：当前 API 分层清晰、REST 用法正确、校验与 record 使用良好。与 Allra 的主要差异在 **DTO 命名**（Request/Response 与 Dto 的区分）和 **dto 子包**（request/response 分离）；按上述建议调整后即可与 Allra API 设计标准对齐，同时保持现有行为不变。
