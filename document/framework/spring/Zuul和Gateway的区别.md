# Zuul 和 Spring Cloud Gateway 的区别

## 1. 一句话总结

> **Zuul** 是 Netflix 提供的网关组件，Spring Cloud 中常见的是 **Zuul 1.x**，基于 Servlet 阻塞模型；  
> **Spring Cloud Gateway** 是 Spring 官方后续主推的网关组件，典型版本基于 Spring WebFlux、Project Reactor、Netty，支持响应式非阻塞模型。Spring Cloud Gateway 官方文档说明它依赖 Spring WebFlux 和 Netty 运行时，不适合以传统 WAR 方式运行在 Servlet 容器中。:contentReference[oaicite:0]{index=0}

---

## 2. 核心区别

| 对比项 | Zuul | Spring Cloud Gateway |
|---|---|---|
| 所属生态 | Netflix OSS | Spring Cloud 官方 |
| 常见版本 | Spring Cloud Netflix Zuul 1.x | Spring Cloud Gateway |
| 编程模型 | Servlet 阻塞模型 | WebFlux / Reactor / Netty 非阻塞模型 |
| 性能模型 | 一个请求通常占用一个线程 | 少量线程处理大量连接 |
| 过滤器模型 | Zuul Filter | GatewayFilter / GlobalFilter |
| 路由配置 | `zuul.routes` | `spring.cloud.gateway.routes` |
| 限流、熔断、监控扩展 | 依赖 Netflix 生态，如 Hystrix、Ribbon | 更容易整合 Spring Cloud LoadBalancer、Resilience4j、Micrometer |
| 维护状态 | Spring Cloud Netflix 多数模块进入维护模式 | Spring Cloud 主推网关方案 |
| 推荐新项目使用 | 不推荐 | 推荐 |

---

## 3. Zuul 是什么

Zuul 是 Netflix 开源的网关服务，定位是边缘服务 / API Gateway。

主要能力：

```text
动态路由
权限校验
监控
限流
灰度发布
安全控制
服务迁移
请求转发
```

Netflix 官方仓库描述 Zuul 是一个 L7 应用网关，提供动态路由、监控、弹性、安全等能力。:contentReference[oaicite:1]{index=1}

在 Spring Cloud Netflix 中，Zuul 被作为路由和服务端负载均衡组件使用。Spring Cloud Netflix 文档也说明，Zuul 是 Netflix 的 JVM-based router 和 server-side load balancer。:contentReference[oaicite:2]{index=2}

---

## 4. Spring Cloud Gateway 是什么

Spring Cloud Gateway 是 Spring Cloud 官方提供的 API Gateway。

它的目标是：

```text
路由请求
统一鉴权
限流
熔断
日志监控
请求改写
响应改写
跨域处理
灰度发布
```

Spring Cloud Gateway 官方项目页说明，它用于在 Spring 生态上构建 API Gateway，并提供安全、监控指标、弹性等横切能力。:contentReference[oaicite:3]{index=3}

---

## 5. 架构模型区别

### 5.1 Zuul 架构模型

Spring Cloud Netflix Zuul 1.x 基于 Servlet 模型。

```text
Client
  ↓
Servlet Container
  ↓
Zuul Servlet
  ↓
Zuul Filter
  ↓
Route To Service
```

Spring Cloud Netflix 文档明确说明，Zuul 是通过 Servlet 实现的。:contentReference[oaicite:4]{index=4}

特点：

```text
同步阻塞
依赖 Servlet 容器
请求线程容易被下游阻塞
高并发连接场景下线程压力较大
```

---

### 5.2 Gateway 架构模型

Spring Cloud Gateway 典型架构基于 WebFlux、Reactor、Netty。

```text
Client
  ↓
Netty
  ↓
WebFlux HandlerMapping
  ↓
Gateway Handler
  ↓
Filter Chain
  ↓
Route To Service
```

特点：

```text
响应式
异步非阻塞
基于事件循环
适合高并发连接场景
```

Spring Cloud Gateway 当前文档说明，该项目基于 Spring 生态，包括 Spring Framework、Spring Boot 和 Project Reactor。:contentReference[oaicite:5]{index=5}

---

## 6. 阻塞和非阻塞区别

### 6.1 Zuul 1.x 阻塞模型

```text
请求进入
  ↓
分配线程
  ↓
调用下游服务
  ↓
线程阻塞等待响应
  ↓
返回结果
```

如果下游服务响应慢，请求线程会被阻塞。

问题：

```text
高并发下线程数压力大
慢接口容易拖垮网关
资源利用率不如非阻塞模型
```

---

### 6.2 Gateway 非阻塞模型

```text
请求进入
  ↓
事件循环处理
  ↓
发起异步调用
  ↓
线程不阻塞
  ↓
下游响应后回调处理
  ↓
返回结果
```

优势：

```text
少量线程处理大量请求
适合高并发长连接场景
资源利用率更高
更符合响应式编程模型
```

---

## 7. 过滤器区别

## 7.1 Zuul Filter

Zuul 过滤器常见类型：

| 类型 | 执行时机 |
|---|---|
| `pre` | 路由前执行 |
| `route` | 路由请求时执行 |
| `post` | 路由后执行 |
| `error` | 出错时执行 |

示例：

```java
@Component
public class AuthFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        HttpServletRequest request = context.getRequest();

        String token = request.getHeader("Authorization");

        if (token == null) {
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(401);
        }

        return null;
    }
}
```

---

## 7.2 Gateway Filter

Spring Cloud Gateway 常见过滤器：

| 类型 | 说明 |
|---|---|
| `GatewayFilter` | 作用于指定路由 |
| `GlobalFilter` | 作用于所有路由 |

示例：

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

---

## 8. 路由配置区别

## 8.1 Zuul 路由配置

```yaml
zuul:
  routes:
    user-service:
      path: /user/**
      serviceId: user-service
```

请求：

```text
/user/1
```

会转发到：

```text
user-service
```

---

## 8.2 Gateway 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
          filters:
            - StripPrefix=1
```

Gateway 的路由由三部分组成：

```text
Route
Predicate
Filter
```

其中：

| 概念 | 说明 |
|---|---|
| Route | 路由规则 |
| Predicate | 匹配条件 |
| Filter | 请求或响应处理逻辑 |

---

## 9. Predicate 和 Filter

Gateway 比 Zuul 更强调 Predicate + Filter 模型。

### Predicate

Predicate 用于判断请求是否匹配某个路由。

常见 Predicate：

```text
Path
Method
Host
Header
Cookie
Query
After
Before
Between
Weight
```

示例：

```yaml
predicates:
  - Path=/api/**
  - Method=GET
```

---

### Filter

Filter 用于对请求和响应做处理。

常见 Filter：

```text
StripPrefix
PrefixPath
AddRequestHeader
AddResponseHeader
RewritePath
RequestRateLimiter
Retry
CircuitBreaker
```

示例：

```yaml
filters:
  - StripPrefix=1
  - AddRequestHeader=X-Request-Source, gateway
```

---

## 10. 性能区别

### Zuul 1.x

```text
基于 Servlet 阻塞 IO
请求线程可能被下游服务阻塞
高并发下线程资源压力较大
```

### Gateway

```text
基于 Netty / WebFlux / Reactor
异步非阻塞
更适合高并发和长连接场景
```

注意：

```text
Gateway 性能更好不是因为“代码一定更快”，而是它的非阻塞模型在高并发连接和慢 IO 场景下资源利用率更高。
```

---

## 11. 生态维护区别

Spring Cloud Netflix 的部分模块进入维护模式，维护模式意味着 Spring Cloud 团队不再给这些模块增加新功能，只修复阻塞级 Bug、安全问题，以及评估社区小型 PR。:contentReference[oaicite:6]{index=6}

实际新项目中，一般推荐：

```text
Spring Cloud Gateway
```

而不是：

```text
Spring Cloud Netflix Zuul
```

---

## 12. Zuul 2 呢？

需要注意：

```text
Zuul 1 和 Zuul 2 不是一回事。
```

Zuul 2 是 Netflix 后来推出的异步非阻塞版本，Netflix 技术博客说明 Zuul 2 与原始 Zuul 的主要架构差异是：Zuul 2 运行在异步非阻塞框架上，并使用 Netty。:contentReference[oaicite:7]{index=7}

但是在 Spring Cloud Netflix 体系中，大家常说的 Zuul 通常指的是：

```text
Spring Cloud Netflix Zuul 1.x
```

也就是 Servlet 阻塞模型。

---

## 13. 使用场景区别

| 场景 | 推荐 |
|---|---|
| 老项目已经使用 Zuul | 可以继续维护，逐步迁移 |
| 新 Spring Cloud 项目 | Spring Cloud Gateway |
| 高并发 API 网关 | Spring Cloud Gateway |
| 需要响应式非阻塞 | Spring Cloud Gateway |
| 项目基于老版 Spring Cloud Netflix | Zuul 可能仍存在 |
| 需要更好地接入新 Spring 生态 | Spring Cloud Gateway |

---

## 14. Gateway 相比 Zuul 的优势

```text
性能模型更先进
Spring 官方主推
支持响应式非阻塞
路由 Predicate 更灵活
Filter 体系更清晰
和 Spring Boot / Spring Cloud 新生态集成更好
更容易整合限流、熔断、监控、链路追踪
配置能力更强
```

---

## 15. Zuul 的优势

```text
早期使用广泛
模型简单
基于 Servlet，传统 Spring MVC 项目更容易理解
老项目迁移成本低
生态历史资料多
```

但新项目一般不建议再选 Zuul。

---

## 16. 常见面试问题

### 16.1 Zuul 和 Gateway 最大区别是什么？

```text
最大区别是底层模型不同。

Zuul 1.x 基于 Servlet，是同步阻塞模型。
Spring Cloud Gateway 基于 WebFlux、Reactor、Netty，是异步非阻塞模型。

所以 Gateway 在高并发、慢 IO、长连接场景下资源利用率更好，也是 Spring Cloud 当前主推的网关方案。
```

---

### 16.2 Zuul 的过滤器有哪些类型？

```text
pre：路由前执行
route：路由时执行
post：路由后执行
error：异常时执行
```

---

### 16.3 Gateway 的核心组件是什么？

```text
Route：路由
Predicate：匹配条件
Filter：过滤器
```

---

### 16.4 Gateway 的过滤器有哪些？

```text
GatewayFilter：作用于某个路由
GlobalFilter：作用于所有路由
```

---

### 16.5 为什么新项目推荐 Gateway？

```text
因为 Spring Cloud Gateway 是 Spring Cloud 官方主推的网关组件，基于 WebFlux / Reactor / Netty，支持异步非阻塞模型，扩展能力强，并且更适合当前 Spring Cloud 生态。
```

---

## 17. 迁移注意点

从 Zuul 迁移到 Gateway 时，要重点关注：

```text
路由配置方式变化
Filter 写法变化
Servlet API 不能直接照搬
RequestContext 替换为 ServerWebExchange
同步阻塞代码要避免直接放入 Gateway Filter
Hystrix / Ribbon 相关配置需要迁移
限流、熔断、重试配置需要重新设计
```

Zuul 中常见：

```java
RequestContext context = RequestContext.getCurrentContext();
HttpServletRequest request = context.getRequest();
```

Gateway 中常见：

```java
ServerHttpRequest request = exchange.getRequest();
ServerHttpResponse response = exchange.getResponse();
```

---

## 18. 总结

Zuul 和 Spring Cloud Gateway 都可以作为微服务网关，用来做统一入口、路由转发、鉴权、限流、日志监控、灰度发布等。

它们最大的区别是底层实现模型不同。Spring Cloud Netflix Zuul 常见的是 Zuul 1.x，基于 Servlet，是同步阻塞模型；请求进入网关后会占用 Servlet 线程，如果下游服务响应慢，线程会被阻塞。Spring Cloud Gateway 是 Spring Cloud 官方主推的网关组件，典型实现基于 WebFlux、Project Reactor 和 Netty，是异步非阻塞模型，更适合高并发和慢 IO 场景。

过滤器方面，Zuul 有 `pre`、`route`、`post`、`error` 四类过滤器；Gateway 则使用 `GatewayFilter` 和 `GlobalFilter`，并通过 Route、Predicate、Filter 组合完成路由匹配和请求处理。

从生态上看，Spring Cloud Netflix 中很多组件已经进入维护模式，新功能不再继续演进，而 Spring Cloud Gateway 是当前 Spring Cloud 体系中更推荐的网关方案。所以老项目如果已经使用 Zuul，可以继续维护或逐步迁移；新项目一般推荐使用 Spring Cloud Gateway。

---

## 19. 一句话总结

> Zuul 1.x 是 Netflix 体系下基于 Servlet 的同步阻塞网关；Spring Cloud Gateway 是 Spring 官方主推的基于 WebFlux / Reactor / Netty 的异步非阻塞网关。新项目通常选 Gateway，老项目中的 Zuul 可以视情况维护或迁移。
