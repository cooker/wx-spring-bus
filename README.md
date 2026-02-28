# wx-spring-bus

基于 JDK 21、Spring Boot 3 的事件调度系统，MongoDB 存储、RabbitMQ 解耦，支持事件发起方与调用链路查看。

## 结构

| 目录 | 说明 |
|------|------|
| [bus](./bus) | 事件总线：消息推送、消费、标记（无 REST，通过公共类 + Spring 事件接入） |
| [man](./man) | 管理端后端：消息管理、重推 |
| [man-vue](./man-vue) | 管理端前端（Vue），对接 man |
| [docs](./docs) | 需求与设计文档 |

## 需求与设计

- **需求文档**：[docs/requirements.md](./docs/requirements.md)（请先确认后再进入开发）
- **事件与存储设计**：[docs/event-and-storage-design.md](./docs/event-and-storage-design.md)
- **Java 开发者手册**：[docs/java-developer-guide.md](./docs/java-developer-guide.md)
- **Bus 组件使用（AOP + 注解与移植）**：[docs/bus-component-usage.md](./docs/bus-component-usage.md)

## 本地运行

- **后端**：需本地 MongoDB、RabbitMQ。`bus` 为**组件**（无启动类），由 `man` 或业务应用引入；`man` 默认 8081。
  ```bash
  mvn -pl man spring-boot:run
  ```
- **前端**：
  ```bash
  cd man-vue && npm i && npm run dev
  ```

## 技术栈

- JDK 21、Spring Boot 3.2.x、MongoDB、RabbitMQ
- 开发模式：DDD
- 前端：Vue 3 + Vite，界面使用 UI UX Pro Max


![](https://cdn.jsdelivr.net/gh/gulugulu-lab/img0@main/2026/02/28/cmr7oA.png)
![](https://cdn.jsdelivr.net/gh/gulugulu-lab/img0@main/2026/02/28/UNdcNx.png)
![](https://cdn.jsdelivr.net/gh/gulugulu-lab/img0@main/2026/02/28/5huqj8.png)
![](https://cdn.jsdelivr.net/gh/gulugulu-lab/img0@main/2026/02/28/DvqP40.png)
![](https://cdn.jsdelivr.net/gh/gulugulu-lab/img0@main/2026/02/28/0SsIUy.png)