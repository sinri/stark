# Stark 1.0.0 使用手册

本文档面向在业务项目中引入 **Stark** 的开发者，说明如何组装运行入口、使用日志与数据访问、以及可选的 HTTP / 队列 / 定时任务组件。

> **版本说明**：当前仓库 `pom.xml` 中构件版本为 `1.0.0-SNAPSHOT`；文档目录 `docs/1.0.0/` 对应计划中的 **1.0.0** 系列 API。若你使用的是正式发布的 `1.0.0` 工件，请以 Maven Central 上实际坐标为准。

---

## 1. Stark 是什么

Stark 是一个基于 **Vert.x 5.0.10** 的轻量 Java 框架，定位为对人与 AI 编码都友好的应用骨架：不依赖 Spring 生态，以 **Future 组合** 组织异步启动流程，并提供日志、MySQL 连接池封装，以及可选的 Web、单节点队列与 Cron 调度等组件。

**根包名**：`io.github.sinri.stark`

---

## 2. 运行环境与依赖引入

### 2.1 环境要求

- **JDK**：25 及以上（与项目编译目标一致）。
- **构建**：Maven；本仓库未附带 `mvnw`，请使用本机安装的 `mvn`。

### 2.2 Maven 坐标

在业务项目的 `pom.xml` 中加入（版本号请替换为你实际使用的发布版或 SNAPSHOT）：

```xml
<dependency>
    <groupId>io.github.sinri</groupId>
    <artifactId>stark</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Stark 会传递引入 Vert.x Core / Web / Web Client / MySQL Client、SLF4J API、JSpecify 等；具体版本以 Stark 发布 POM 为准。

---

## 3. 程序入口：`StarkProgram`

业务应用通常继承 `io.github.sinri.stark.program.StarkProgram`，在 `main` 中调用 `delegateMain(args)`。框架按固定顺序执行异步链：

1. `parseArgs` — 解析命令行参数  
2. `buildStark` — 构造 `Stark`（包装 `Vertx`）  
3. `buildLoggerFactory` — 可选：替换全局 `LoggerFactory`；返回 `null` 表示保持默认  
4. `buildQueueDispatcher` — 可选：部署队列调度 Verticle  
5. `buildCronJobDispatcher` — 可选：部署 Cron 调度 Verticle  
6. `buildHTTPService` — 可选：部署 HTTP 服务 Verticle  

启动全部成功后，主线程在内部 `CountDownLatch` 上等待；当各服务卸载完成（例如进程收到关闭信号后 Verticle 停止）后才会继续退出。

**你需要实现的方法**（均为 `protected abstract`）：

| 方法 | 含义 |
|------|------|
| `Future<Void> parseArgs(String[] args)` | 参数解析 |
| `Future<Stark> buildStark()` | 创建 `Stark`，一般内部 `Vertx.vertx()` 再 `new Stark(vertx)` |
| `Future<LoggerFactory> buildLoggerFactory()` | 自定义日志工厂，不需要则 `Future.succeededFuture(null)` |
| `Future<QueueDispatcher> buildQueueDispatcher()` | 不需要队列则 `Future.succeededFuture(null)` |
| `Future<CronJobDispatcher> buildCronJobDispatcher()` | 不需要定时任务则 `Future.succeededFuture(null)` |
| `Future<HTTPService> buildHTTPService()` | 不需要 HTTP 则 `Future.succeededFuture(null)` |

示例骨架（省略具体实现）：

```java
public class MyApp extends StarkProgram {
    public static void main(String[] args) throws InterruptedException {
        new MyApp().delegateMain(args);
    }

    @Override
    protected Future<Void> parseArgs(String[] args) {
        return Future.succeededFuture();
    }

    @Override
    protected Future<Stark> buildStark() {
        return Future.succeededFuture(new Stark(Vertx.vertx()));
    }

    @Override
    protected Future<LoggerFactory> buildLoggerFactory() {
        return Future.succeededFuture(null);
    }

    @Override
    protected Future<QueueDispatcher> buildQueueDispatcher() {
        return Future.succeededFuture(null);
    }

    @Override
    protected Future<CronJobDispatcher> buildCronJobDispatcher() {
        return Future.succeededFuture(null);
    }

    @Override
    protected Future<HTTPService> buildHTTPService() {
        return Future.succeededFuture(null);
    }
}
```

---

## 4. `Stark` 与 Vert.x

`io.github.sinri.stark.core.Stark` 继承 Vert.x 的 `VertxWrapper` 并实现 `StartAsyncMixin`，因此：

- 在任意接受 `Vertx` 的 API 中，通常可以传入 `Stark` 实例。  
- 可使用 `StartAsyncMixin` 提供的异步辅助方法，例如：`asyncSleep`、`asyncCallRepeatedly`、`asyncCallIteratively`、`asyncCallStepwise`、`asyncCallEndlessly` 等（通过 `Stark` 实例调用）。

Verticle 部署请优先在 Stark 体系内使用 `StarkVerticle` / `StarkVerticleBase`（见下文）。

---

## 5. 数据表示：`DataEntity`

`io.github.sinri.stark.core.DataEntity` 继承 Vert.x 的 `JsonObject`，作为框架内常用的结构化数据载体。日志模型 `Log` 也继承自 `DataEntity`。

业务上可将查询结果或消息体映射为 `JsonObject` / `DataEntity`，按需 `put` / `get`；框架不内置 ORM，SQL 仍通过 Vert.x SQL Client 直接执行。

---

## 6. 日志子系统

### 6.1 模型：`Log` 与 `Logger`

- `io.github.sinri.stark.logging.base.Log`：一条日志事件，内含时间戳、级别、消息、线程名、可选 `context`（`JsonObject`）、可选 `Throwable`，以及通过 `setExtraItem` 添加的扩展字段（保留键名不可占用）。  
- `io.github.sinri.stark.logging.base.Logger`：按 **topic**（通常为类名）输出日志；提供 `trace` / `debug` / `info` / `warn` / `error` 及接受 `Handler<Log>` 的重载。

### 6.2 全局工厂：`LoggerFactory`

- `LoggerFactory.universal()`：获取当前全局工厂（默认实现为控制台输出）。  
- `LoggerFactory.universal(LoggerFactory factory)`：替换全局工厂；适合在 `StarkProgram.buildLoggerFactory` 中返回你的工厂。  
- `createLogger(String topic)` / `createLogger(Class<?> clazz)`：按主题创建 `Logger`。

默认实现为 `StdoutLoggerFactory` + `StdoutLogProcesser`，开箱即用。

### 6.3 扩展：渲染与输出

- `LogRender<R>`：将 `Log` 渲染为某种表示（如纯文本、JSON）。  
- `LogProcesser`：`process(String topic, Log log)` 负责实际输出；可同步（返回 `null`）或异步（返回 `Future<Void>`）。

自定义日志管道时，可实现自己的 `LogProcesser` 与 `LoggerFactory`，在应用启动早期注册到 `LoggerFactory.universal(...)`。

### 6.4 SLF4J 桥接

包 `io.github.sinri.stark.logging.slf4j` 提供 SLF4J 的 ServiceProvider 实现，使第三方库通过 SLF4J 打出的日志可接入 Stark 日志栈（需在 classpath 与 `module-info` / SPI 配置中按 SLF4J 2.x 约定启用）。

### 6.5 阿里云 SLS（可选）

包 `io.github.sinri.stark.logging.aliyun.sls.putter` 提供向阿里云日志服务投递日志的相关类型（如 `SlsLogger`、`SlsLogProcesser` 等）。若你的部署环境使用 SLS，可在此包基础上配置 endpoint、凭证与 project/logstore，再挂入自定义 `LoggerFactory`。

---

## 7. MySQL：`StarkMySQLPool`

`io.github.sinri.stark.database.mysql.StarkMySQLPool` 是对 Vert.x **响应式 MySQL 客户端** `Pool` 的扩展，并带**按名称注册表**：

- `StarkMySQLPool.create(vertx, poolName, sqlConnectOptions, poolOptions)`：创建池并注册到 `poolName`。  
- `StarkMySQLPool.registered(poolName)`：按名查找已注册池。  
- `register` / `unregister`：手动注册或移除映射；同名已存在时 `register` 会抛 `IllegalStateException`。  
- `close()`：**幂等**；关闭底层连接并从注册表移除该名（若仍指向本池）。

典型用法：在 `buildStark` 之后创建若干命名池，供各 Verticle 或业务模块通过名称获取，避免在全局散落 `Pool` 引用。

连接参数使用 Vert.x 的 `SqlConnectOptions`、`PoolOptions`（例如 `setMaxSize`）。

---

## 8. HTTP 服务：`HTTPService`

继承 `io.github.sinri.stark.component.web.HTTPService`：

1. 实现 `buildHttpServerOptions()`：返回 `HttpServerOptions`（端口、主机等）。  
2. 实现 `configureRoutes(Router router)`：使用 Vert.x Web 的 `Router` 注册路由。  
3. 可选重写 `beforeStartServer()` / `afterShutdownServer()` 做启动前准备与关闭后清理。

部署：`httpService.deployMe(stark)`。框架使用 **虚拟线程** 模型（`ThreadingModel.VIRTUAL_THREAD`）部署该 Verticle。

最小示例（与测试代码思路一致）：

```java
public class ApiHttpService extends HTTPService {
    private final int port;

    public ApiHttpService(int port) {
        this.port = port;
    }

    @Override
    protected HttpServerOptions buildHttpServerOptions() {
        return new HttpServerOptions().setPort(port).setHost("127.0.0.1");
    }

    @Override
    protected void configureRoutes(Router router) {
        router.get("/ping").handler(ctx -> ctx.response().end("pong"));
    }
}
```

---

## 9. 单节点队列：`QueueDispatcher` 与 `QueueTask`

> **适用范围**：文档与源码均说明队列组件**仅适合单节点**场景。

### 9.1 `QueueDispatcher`

抽象类 `io.github.sinri.stark.component.queue.QueueDispatcher` 继承 `StarkVerticleBase`，并实现：

- `NextQueueTaskSeeker`：`Future<QueueTask> seekNextTask()` — 找下一个待执行任务，且**约定调用方已实现锁定**；无任务时返回 `Future.succeededFuture(null)`。  
- `QueueSignalReader`：`Future<QueueSignal> readSignal()` — 返回 `RUN` 或 `STOP`，用于暂停/继续消费。

还可重写：

- `getWaitingPeriodInMsWhenTaskFree()`：无任务时下一轮轮询间隔（默认 10 秒）。  
- `buildQueueWorkerPoolManager()`：并发控制（默认 `QueueWorkerPoolManager(0)` 表示不限制并发，具体语义见该类 Javadoc）。  
- `beforeQueueStart()`：队列进入运行态前的准备。

部署使用 **WORKER** 线程模型：`deployMe(Stark)`。

### 9.2 `QueueTask`

业务任务继承 `io.github.sinri.stark.component.queue.QueueTask`，实现：

- `getTaskReference()` / `getTaskCategory()`  
- `buildQueueTaskLogger()`  
- `run()`：任务主体异步逻辑  

任务部署后会在 `run()` 链路结束后自动 `undeployMe()`。可通过 `expectedThreadingModel()` 调整线程模型（默认 `WORKER`）。

---

## 10. 定时任务（日晷）：`CronJobDispatcher`

`io.github.sinri.stark.component.sundial.CronJobDispatcher` 在**单节点**内按分钟粒度匹配 Cron 表达式，命中后为对应计划部署一个 `CronJobVerticle` 实例。

你需要：

1. 继承 `CronJobDispatcher`，实现 `fetchPlans()`：返回 `Future<Collection<CronJobPlan>>`；若返回 **`null`** 表示本次不更新内存中的计划快照。  
2. 定义 `CronJobPlan`：提供 `key()`、`cronExpression()`（`CronExpression`，精确到分钟）、`triggerVerticleClass()`（带 `Calendar` 构造函数的 `CronJobVerticle` 子类）。  

`CronJobVerticle` 抽象子类需实现 `startVerticle()` 等（基类提供 `getTriggerTime()` 与 `getLogger()`）。

调度器以 **WORKER** 模型部署；内部使用共享数据锁刷新计划，避免并发刷新冲突。

---

## 11. Verticle 基础：`StarkVerticle` / `StarkVerticleBase`

- `StarkVerticle`：Stark 约定的 Verticle 接口，包含 `init`、`start`、`stop`、`deployMe`、`undeployMe`、`undeployed()` 等。  
- `StarkVerticleBase`：推荐基类；**每个实例仅允许部署一次**。提供 `wrap(...)` 用函数式快速包装启动/停止逻辑。

`init` 时会把 `Vertx` 转为 `Stark` 保存，后续通过 `getStark()` 使用。

---

## 12. 开发约定（摘要）

以下为框架自身编码约定，业务代码建议对齐以便维护：

- **可空性**：各包通过 `package-info.java` 使用 JSpecify `@NullMarked`；可空处使用 `@Nullable`。  
- **不使用 Lombok**：手写 getter/setter。  
- **无 ORM**：SQL 走 Vert.x 客户端；领域对象可用 `DataEntity` / `JsonObject`。  
- **Web**：每个 API 端点一个最终 Handler 类（业务侧自行组织路由注册）。  
- **测试**：JUnit 5，测试类名 `<被测类>Test`。

更完整的构建命令与架构说明可参考仓库根目录的 `CLAUDE.md`。

---

## 13. 常见问题

**Q：为什么 `delegateMain` 会一直阻塞？**  
A：设计上主线程等待服务生命周期结束；需通过 Vert.x 关闭流程或卸载 Verticle，使 `undeployed()` 等 Future 完成，才会触发退出。

**Q：能否不用 `StarkProgram`？**  
A：可以，但不建议，常规项目直接使用 StarkProgram 更加方便。你可自行设计创建 `Stark` 并部署 `HTTPService` / `QueueDispatcher` 等；`StarkProgram` 只是统一启动顺序与日志工厂切换的模板。

**Q：`StarkMySQLPool` 与直接 `MySQLPool.pool(...)` 的区别？**  
A：Stark 在 Vert.x Pool 之上增加了**命名注册**与**关闭时注销**，便于在模块化代码中按名称解析连接池。

---

## 14. 参考与延伸阅读

| 资源 | 说明 |
|------|------|
| 仓库 `CLAUDE.md` | 构建命令与架构速览 |
| `docs/1.0.0/设计与实现分析报告.md` | 设计与实现分析（若存在） |
| [Vert.x 5 文档](https://vertx.io/docs/) | 路由、SQL Client、部署模型等 |

---

*文档与 Stark 源码同步演进；若你发现与代码不一致之处，以当前分支源码为准并欢迎提 Issue 或 PR 修正本文档。*
