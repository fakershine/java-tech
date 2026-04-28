# Tomcat 总结

Tomcat 是一个 **Servlet 容器 / Web 容器**，主要用于运行 Java Web 应用。

核心作用：

```text
接收 HTTP 请求
  ↓
解析请求
  ↓
调用 Servlet / Spring MVC
  ↓
返回 HTTP 响应
```

---

## 1. Tomcat 是什么

Tomcat 本质上是：

```text
HTTP 服务器 + Servlet 容器
```

它可以：

- 监听端口。
- 接收 HTTP 请求。
- 解析请求参数。
- 管理 Servlet 生命周期。
- 调用业务代码。
- 返回 HTTP 响应。

在 Spring Boot 中，默认内嵌的 Web 容器通常就是 Tomcat。

---

## 2. Tomcat 核心组件

| 组件 | 作用 |
|---|---|
| Server | Tomcat 顶层组件，代表整个 Tomcat 实例 |
| Service | 连接 Connector 和 Container |
| Connector | 接收客户端请求 |
| Container | 处理请求 |
| Engine | Servlet 引擎 |
| Host | 虚拟主机 |
| Context | 一个 Web 应用 |
| Wrapper | 一个 Servlet |

结构：

```text
Server
  ↓
Service
  ├── Connector
  └── Container
        ↓
      Engine
        ↓
      Host
        ↓
      Context
        ↓
      Wrapper
```

---

## 3. Connector

### 作用

Connector 负责网络通信。

主要职责：

```text
监听端口
接收 TCP 连接
解析 HTTP 请求
封装 Request / Response
把请求交给 Container
```

常见协议：

```text
HTTP/1.1
AJP
```

常见 IO 模型：

```text
BIO
NIO
APR
```

现在常用：

```text
NIO
```

---

## 4. Container

### 作用

Container 负责处理请求，并找到对应的 Servlet。

处理链路：

```text
Engine
  ↓
Host
  ↓
Context
  ↓
Wrapper
  ↓
Servlet
```

对应关系：

```text
Engine：整个 Servlet 引擎
Host：一个虚拟主机
Context：一个 Web 应用
Wrapper：一个 Servlet
```

---

## 5. Tomcat 请求处理流程

```text
客户端发送 HTTP 请求
  ↓
Connector 接收连接
  ↓
解析 HTTP 请求
  ↓
封装 Request 和 Response
  ↓
交给 Container
  ↓
匹配 Host
  ↓
匹配 Context
  ↓
匹配 Servlet
  ↓
执行 Filter 链
  ↓
调用 Servlet
  ↓
返回响应
```

如果是 Spring MVC：

```text
请求
  ↓
Tomcat
  ↓
DispatcherServlet
  ↓
HandlerMapping
  ↓
Controller
  ↓
Service
  ↓
Response
```

---

## 6. Servlet 生命周期

Tomcat 负责管理 Servlet 生命周期。

```text
加载 Servlet 类
  ↓
实例化 Servlet
  ↓
调用 init()
  ↓
处理请求 service()
  ↓
关闭时调用 destroy()
```

核心方法：

| 方法 | 说明 |
|---|---|
| `init()` | Servlet 初始化 |
| `service()` | 处理请求 |
| `destroy()` | Servlet 销毁 |

---

## 7. Tomcat 线程模型

Tomcat 会使用线程池处理请求。

简化流程：

```text
Acceptor 接收连接
  ↓
Poller 监听 IO 事件
  ↓
Worker 线程处理请求
```

NIO 模型下：

| 线程 | 作用 |
|---|---|
| Acceptor | 接收客户端连接 |
| Poller | 监听连接上的读写事件 |
| Worker | 处理具体请求 |

---

## 8. 关键参数

| 参数 | 说明 |
|---|---|
| `server.port` | 服务端口 |
| `maxThreads` | 最大工作线程数 |
| `minSpareThreads` | 最小空闲线程数 |
| `acceptCount` | 等待队列长度 |
| `maxConnections` | 最大连接数 |
| `connectionTimeout` | 连接超时时间 |
| `keepAliveTimeout` | KeepAlive 超时时间 |

Spring Boot 示例：

```yaml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 20
    accept-count: 100
    max-connections: 8192
    connection-timeout: 20s
```

---

## 9. maxThreads

### 作用

`maxThreads` 表示 Tomcat 最大工作线程数。

```text
请求真正被业务处理时，需要占用 Worker 线程
```

如果线程数不够：

```text
请求排队
RT 升高
甚至超时
```

调优建议：

```text
CPU 密集型：线程数不宜过大
IO 密集型：可以适当调大
```

---

## 10. acceptCount

### 作用

当工作线程都忙，且连接数达到上限后，新请求会进入等待队列。

`acceptCount` 表示等待队列长度。

```text
线程满
  ↓
请求进入队列
  ↓
队列也满
  ↓
新请求被拒绝
```

---

## 11. maxConnections

### 作用

表示 Tomcat 能同时建立的最大连接数。

```text
连接数过大：占用内存和 FD
连接数过小：高并发下连接被拒绝
```

需要结合：

```text
系统 fd 限制
机器内存
请求耗时
QPS
```

一起调优。

---

## 12. Tomcat 和 Spring MVC 关系

Tomcat 是 Web 容器。

Spring MVC 的核心是：

```text
DispatcherServlet
```

关系：

```text
Tomcat 接收 HTTP 请求
  ↓
Tomcat 调用 DispatcherServlet
  ↓
DispatcherServlet 分发到 Controller
```

简单理解：

```text
Tomcat 负责接请求
Spring MVC 负责处理业务路由
```

---

## 13. Tomcat 和 Nginx 区别

| 对比项 | Nginx | Tomcat |
|---|---|---|
| 定位 | Web 服务器 / 反向代理 | Servlet 容器 |
| 主要作用 | 静态资源、代理、负载均衡 | 运行 Java Web 应用 |
| 性能 | 静态资源性能强 | 动态 Java 请求处理 |
| 是否执行 Java 代码 | 否 | 是 |

常见架构：

```text
客户端
  ↓
Nginx
  ↓
Tomcat
  ↓
Java 应用
```

---

## 14. Tomcat 常见优化

### 线程池优化

```text
合理设置 maxThreads
避免线程过多导致上下文切换
```

### 连接数优化

```text
调整 maxConnections
调整 acceptCount
调整 keepAliveTimeout
```

### JVM 优化

```text
合理设置 Xms / Xmx
选择合适 GC
观察 GC 日志
```

### 静态资源优化

```text
静态资源交给 Nginx / CDN
Tomcat 专注处理动态请求
```

### 超时优化

```text
设置连接超时
设置业务接口超时
避免慢请求占满线程
```

---

## 15. 常见问题

### 1. Tomcat 线程打满会怎样？

表现：

```text
接口变慢
请求排队
RT 升高
504 超时
```

原因可能是：

```text
慢 SQL
下游接口慢
锁竞争
线程池过小
请求量过大
```

---

### 2. Tomcat 能处理多少并发？

取决于：

```text
机器配置
maxThreads
maxConnections
接口耗时
数据库能力
Redis 能力
下游服务能力
```

不能只看 Tomcat 参数。

---

### 3. Spring Boot 为什么不用外部 Tomcat？

Spring Boot 默认内嵌 Tomcat。

优势：

```text
部署简单
直接 java -jar 启动
环境一致
适合微服务
```

---

## 16. 总结

Tomcat 是一个 Servlet 容器，负责接收 HTTP 请求、解析请求、封装 Request 和 Response，并调用对应的 Servlet 处理请求。

Tomcat 的核心组件包括 Server、Service、Connector、Container、Engine、Host、Context、Wrapper。Connector 负责网络连接和协议解析，Container 负责请求处理和 Servlet 调用。

在 Spring MVC 项目中，请求先进入 Tomcat，再由 Tomcat 调用 `DispatcherServlet`，最后分发到具体 Controller。

一句话总结：

```text
Tomcat = HTTP 服务器 + Servlet 容器；
Connector 负责接请求，Container 负责找 Servlet 并执行。
```
