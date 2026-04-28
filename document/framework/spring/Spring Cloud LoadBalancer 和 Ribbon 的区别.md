# Spring Cloud LoadBalancer 和 Ribbon 的区别

## 1. 一句话总结

> **Ribbon** 是 Netflix OSS 体系中的客户端负载均衡组件，早期 Spring Cloud 常用；  
> **Spring Cloud LoadBalancer** 是 Spring Cloud 官方提供的新一代客户端负载均衡组件，用来替代 Ribbon。Spring 官方博客也明确说明，Spring Cloud LoadBalancer 是一个通用抽象，可以完成以前 Netflix Ribbon 做的工作。:contentReference[oaicite:0]{index=0}

---

## 2. 核心区别

| 对比项 | Ribbon | Spring Cloud LoadBalancer |
|---|---|---|
| 所属体系 | Netflix OSS | Spring Cloud 官方 |
| 维护状态 | 已进入维护模式，不推荐新项目继续使用 | Spring Cloud 当前推荐方案 |
| 负载均衡位置 | 客户端负载均衡 | 客户端负载均衡 |
| 默认集成时代 | 老版 Spring Cloud Netflix | 新版 Spring Cloud |
| 编程模型 | 主要偏阻塞式调用 | 同时支持阻塞和响应式 |
| 常见客户端 | RestTemplate、Feign | RestTemplate、WebClient、Feign |
| 服务发现 | 常和 Eureka 搭配 | 可和 Eureka、Consul、Zookeeper 等 DiscoveryClient 搭配 |
| 扩展方式 | `IRule`、`ILoadBalancer`、`ServerList` | `ReactorLoadBalancer`、`ServiceInstanceListSupplier` |
| Spring 生态适配 | 老生态 | 新 Spring Boot / Spring Cloud 生态更自然 |
| 新项目推荐 | 不推荐 | 推荐 |

---

## 3. 它们都是什么类型的负载均衡

Ribbon 和 Spring Cloud LoadBalancer 都属于：

```text
客户端负载均衡
```

也就是说，服务调用方自己从服务注册中心拿到服务实例列表，然后在本地选择一个实例发起请求。

流程：

```text
服务调用方
   ↓
从注册中心获取服务实例列表
   ↓
本地负载均衡算法选择一个实例
   ↓
发起 HTTP / RPC 调用
```

和 Nginx 这种服务端负载均衡不同：

```text
客户端 -> Nginx -> 后端服务实例
```

客户端负载均衡是：

```text
客户端自己选择服务实例
```

---

# 一、Ribbon

## 4. Ribbon 是什么

Ribbon 是 Netflix 提供的客户端负载均衡组件。

Spring Cloud Netflix 文档中说明，Ribbon 是一个客户端负载均衡器，可以对 HTTP 和 TCP 客户端行为提供较多控制；并且 Feign 也曾经使用 Ribbon，因此使用 `@FeignClient` 时相关配置也会涉及 Ribbon。:contentReference[oaicite:1]{index=1}

核心能力：

```text
服务实例列表维护
客户端负载均衡
重试
服务实例选择
和 Eureka 集成
```

---

## 5. Ribbon 调用流程

```text
服务 A 调用服务 B
   ↓
Ribbon 从 Eureka 获取服务 B 的实例列表
   ↓
根据负载均衡规则选择一个实例
   ↓
RestTemplate / Feign 发起请求
```

示例：

```text
user-service 有 3 个实例：

192.168.1.10:8080
192.168.1.11:8080
192.168.1.12:8080

Ribbon 根据规则选择其中一个实例调用
```

---

## 6. Ribbon 常见核心组件

| 组件 | 说明 |
|---|---|
| `ILoadBalancer` | 负载均衡器接口 |
| `IRule` | 负载均衡规则接口 |
| `ServerList` | 服务实例列表 |
| `ServerListFilter` | 服务列表过滤器 |
| `IPing` | 检测服务实例是否可用 |
| `RibbonLoadBalancerClient` | Spring Cloud 对 Ribbon 的封装 |

---

## 7. Ribbon 常见负载均衡规则

| 规则 | 说明 |
|---|---|
| `RoundRobinRule` | 轮询 |
| `RandomRule` | 随机 |
| `RetryRule` | 重试 |
| `WeightedResponseTimeRule` | 根据响应时间加权 |
| `BestAvailableRule` | 选择并发请求较少的实例 |
| `AvailabilityFilteringRule` | 过滤不可用实例 |
| `ZoneAvoidanceRule` | 区域感知，默认常见规则之一 |

---

## 8. Ribbon 配置示例

```yaml
user-service:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```

表示调用 `user-service` 时使用随机策略。

---

# 二、Spring Cloud LoadBalancer

## 9. Spring Cloud LoadBalancer 是什么

Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的客户端负载均衡抽象和实现。

它可以配合：

```text
RestTemplate
WebClient
RestClient
OpenFeign
Spring Cloud Gateway
DiscoveryClient
```

Spring Cloud Commons 官方文档说明，Spring Cloud LoadBalancer 支持基于服务发现的 `ServiceInstanceListSupplier` 实现，可以通过 classpath 中的 Discovery Client 获取可用服务实例。:contentReference[oaicite:2]{index=2}

---

## 10. Spring Cloud LoadBalancer 调用流程

```text
服务 A 调用服务 B
   ↓
LoadBalancerClient / ReactorLoadBalancer
   ↓
ServiceInstanceListSupplier 获取服务实例列表
   ↓
负载均衡算法选择一个 ServiceInstance
   ↓
发起请求
```

示例：

```text
http://user-service/user/1
   ↓
LoadBalancer 解析 user-service
   ↓
从注册中心获取 user-service 实例
   ↓
选择 192.168.1.10:8080
   ↓
实际请求 http://192.168.1.10:8080/user/1
```

---

## 11. Spring Cloud LoadBalancer 核心组件

| 组件 | 说明 |
|---|---|
| `LoadBalancerClient` | 阻塞式负载均衡客户端 |
| `ReactiveLoadBalancer` | 响应式负载均衡接口 |
| `ReactorLoadBalancer` | Reactor 风格负载均衡器 |
| `ServiceInstanceListSupplier` | 服务实例列表供应器 |
| `ServiceInstance` | 服务实例信息 |
| `LoadBalancerClientFactory` | 为不同 serviceId 创建负载均衡上下文 |

Spring Cloud LoadBalancer 会为每个 service id 创建独立的 Spring 子上下文，默认是懒加载，也可以通过 `spring.cloud.loadbalancer.eager-load.clients` 配置提前加载。:contentReference[oaicite:3]{index=3}

---

## 12. Spring Cloud LoadBalancer 支持的客户端

### 12.1 RestTemplate

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

使用：

```java
String result = restTemplate.getForObject(
        "http://user-service/user/1",
        String.class
);
```

这里的 `user-service` 是服务名，不是真实域名。

---

### 12.2 WebClient

```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

使用：

```java
Mono<String> result = webClientBuilder.build()
        .get()
        .uri("http://user-service/user/1")
        .retrieve()
        .bodyToMono(String.class);
```

Spring Cloud Commons 文档说明，使用 `@LoadBalanced WebClient.Builder` 时，URI 需要使用虚拟主机名，也就是服务名，而不是具体主机名；Spring Cloud LoadBalancer 会用真实物理地址替换它。:contentReference[oaicite:4]{index=4}

---

### 12.3 OpenFeign

```java
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/user/{id}")
    UserDTO getById(@PathVariable("id") Long id);
}
```

Feign 调用时，会根据服务名 `user-service` 走负载均衡。

---

# 三、为什么 Ribbon 被替代

## 13. Ribbon 已进入维护模式

Spring Cloud Netflix 相关说明中提到，Ribbon 负载均衡器已进入维护模式，建议切换到 Spring Cloud LoadBalancer。:contentReference[oaicite:5]{index=5}

Spring Cloud Netflix 的维护模式意味着不再为这些模块增加新功能，通常只处理阻塞级 bug、安全问题以及少量社区 PR。:contentReference[oaicite:6]{index=6}

所以新项目一般不再推荐 Ribbon。

---

## 14. Spring Cloud 为什么推出 LoadBalancer

主要原因：

```text
减少对 Netflix OSS 老组件依赖
更贴合 Spring Boot / Spring Cloud 新生态
支持响应式编程模型
统一负载均衡抽象
更容易和 WebClient、Gateway、OpenFeign 集成
长期维护更稳定
```

Spring 官方博客也提到，虽然 Spring Cloud 仍支持 Netflix Ribbon，但 Ribbon 的日子已经进入倒计时，因此 Spring 提供了 Spring Cloud LoadBalancer 作为替代抽象。:contentReference[oaicite:7]{index=7}

---

# 四、架构区别

## 15. Ribbon 架构

```text
RestTemplate / Feign
   ↓
RibbonLoadBalancerClient
   ↓
ILoadBalancer
   ↓
IRule
   ↓
ServerList
   ↓
选择 Server
```

核心是：

```text
ILoadBalancer + IRule + Server
```

---

## 16. Spring Cloud LoadBalancer 架构

```text
RestTemplate / WebClient / Feign
   ↓
LoadBalancerClient / ReactorLoadBalancer
   ↓
ServiceInstanceListSupplier
   ↓
ServiceInstance
   ↓
选择实例
```

核心是：

```text
ReactorLoadBalancer + ServiceInstanceListSupplier + ServiceInstance
```

---

## 17. 两者核心模型对比

| 对比项 | Ribbon | Spring Cloud LoadBalancer |
|---|---|---|
| 服务实例对象 | `Server` | `ServiceInstance` |
| 实例列表来源 | `ServerList` | `ServiceInstanceListSupplier` |
| 负载均衡算法 | `IRule` | `ReactorLoadBalancer` |
| 阻塞支持 | 支持 | 支持 |
| 响应式支持 | 弱 | 支持更好 |
| Spring Cloud 生态 | 老方案 | 新方案 |

---

# 五、负载均衡策略区别

## 18. Ribbon 策略

Ribbon 内置策略较多：

```text
轮询
随机
重试
加权响应时间
区域感知
可用性过滤
最小并发
```

自定义策略通常实现：

```java
IRule
```

示例：

```java
public class MyRule extends AbstractLoadBalancerRule {

    @Override
    public Server choose(Object key) {
        return getLoadBalancer().chooseServer(key);
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
    }
}
```

---

## 19. Spring Cloud LoadBalancer 策略

Spring Cloud LoadBalancer 常见默认策略是轮询。

也可以配置随机策略或自定义 `ReactorLoadBalancer`。

示例：随机策略配置思路：

```java
@Configuration
public class CustomLoadBalancerConfiguration {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory
    ) {
        String name = environment.getProperty(
                LoadBalancerClientFactory.PROPERTY_NAME
        );

        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(
                        name,
                        ServiceInstanceListSupplier.class
                ),
                name
        );
    }
}
```

---

## 20. 自定义扩展区别

| 对比项 | Ribbon | Spring Cloud LoadBalancer |
|---|---|---|
| 自定义实例选择 | 实现 `IRule` | 实现 `ReactorLoadBalancer` |
| 自定义实例列表 | 实现 `ServerList` | 实现 `ServiceInstanceListSupplier` |
| 自定义过滤 | `ServerListFilter` | 包装 / 扩展 `ServiceInstanceListSupplier` |
| 风格 | Netflix 风格 | Spring Bean 风格 |
| 配置方式 | Ribbon 客户端配置 | Spring Boot 自动配置 + Bean |

---

# 六、配置区别

## 21. Ribbon 配置方式

```yaml
user-service:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
    ConnectTimeout: 3000
    ReadTimeout: 5000
```

---

## 22. Spring Cloud LoadBalancer 配置方式

依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

RestTemplate：

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

WebClient：

```java
@Bean
@LoadBalanced
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}
```

---

## 23. 禁用 Ribbon 切换到 LoadBalancer

在部分老版本 Spring Cloud 中，为了兼容 Ribbon，可能需要显式关闭 Ribbon：

```yaml
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false
```

Spring Cloud Netflix 相关说明中也提到，由于 Ribbon 已进入维护模式，建议切换到 Spring Cloud LoadBalancer，并可以设置 `spring.cloud.loadbalancer.ribbon.enabled=false`。:contentReference[oaicite:8]{index=8}

---

# 七、和 Feign 的关系

## 24. 早期 Feign + Ribbon

早期 Spring Cloud OpenFeign 默认常见组合是：

```text
OpenFeign + Ribbon + Eureka
```

调用流程：

```text
Feign 接口
   ↓
Ribbon 选择服务实例
   ↓
HTTP 调用
```

---

## 25. 现在 Feign + LoadBalancer

新版 Spring Cloud 更推荐：

```text
OpenFeign + Spring Cloud LoadBalancer + DiscoveryClient
```

调用流程：

```text
Feign 接口
   ↓
Spring Cloud LoadBalancer 选择服务实例
   ↓
HTTP 调用
```

---

# 八、和 Gateway 的关系

## 26. Spring Cloud Gateway 使用 LoadBalancer

Spring Cloud Gateway 中常见配置：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
```

这里：

```text
lb://user-service
```

表示通过负载均衡选择 `user-service` 的一个实例。

在新生态下，这通常由 Spring Cloud LoadBalancer 完成。

---

# 九、两者优缺点

## 27. Ribbon 优点

```text
使用历史长
资料多
内置负载均衡规则较丰富
和老版 Spring Cloud Netflix 结合紧密
```

---

## 28. Ribbon 缺点

```text
进入维护模式
Netflix OSS 老生态
响应式支持弱
新 Spring Cloud 版本中不再推荐
后续演进有限
```

---

## 29. Spring Cloud LoadBalancer 优点

```text
Spring Cloud 官方维护
新项目推荐
支持阻塞和响应式调用
和 WebClient / Gateway / OpenFeign 适配更自然
基于 Spring Boot 自动配置和 Bean 扩展
更适合当前 Spring Cloud 生态
```

---

## 30. Spring Cloud LoadBalancer 缺点

```text
相比 Ribbon，早期内置策略没有 Ribbon 那么多
老项目迁移需要改配置和依赖
部分高级策略需要自己扩展
```

---

# 十、如何选型

## 31. 新项目

推荐：

```text
Spring Cloud LoadBalancer
```

原因：

```text
Spring Cloud 官方维护
更符合当前 Spring Cloud 生态
Ribbon 已进入维护模式
支持 WebClient 和响应式调用
```

---

## 32. 老项目

如果老项目已经使用：

```text
Ribbon + Eureka + OpenFeign
```

可以：

```text
短期继续维护
中长期逐步迁移到 Spring Cloud LoadBalancer
```

迁移时重点关注：

```text
依赖替换
Ribbon 配置替换
自定义 IRule 迁移
Feign 调用是否正常
Gateway 路由是否正常
超时和重试策略重新配置
```

---

# 十一、常见面试问题

## 33. Spring Cloud LoadBalancer 和 Ribbon 最大区别是什么？

```text
最大区别是所属生态和维护状态。

Ribbon 是 Netflix OSS 的客户端负载均衡组件，早期 Spring Cloud 常用，但现在已经进入维护模式。
Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的新客户端负载均衡组件，是当前推荐方案。

从扩展模型看，Ribbon 主要基于 IRule、ILoadBalancer、ServerList；
Spring Cloud LoadBalancer 主要基于 ReactorLoadBalancer、ServiceInstanceListSupplier、ServiceInstance。
```

---

## 34. 它们是客户端负载均衡还是服务端负载均衡？

```text
都是客户端负载均衡。
```

调用方自己从注册中心拿到实例列表，然后在本地选择一个实例调用。

---

## 35. Ribbon 常见负载均衡规则有哪些？

```text
RoundRobinRule：轮询
RandomRule：随机
RetryRule：重试
WeightedResponseTimeRule：按响应时间加权
BestAvailableRule：选择可用且并发少的实例
AvailabilityFilteringRule：过滤不可用实例
ZoneAvoidanceRule：区域感知
```

---

## 36. Spring Cloud LoadBalancer 如何自定义负载均衡策略？

常见方式：

```text
自定义 ReactorLoadBalancer
自定义 ServiceInstanceListSupplier
通过 @LoadBalancerClient 指定某个服务的配置
```

---

## 37. `@LoadBalanced` 是什么作用？

`@LoadBalanced` 用在 `RestTemplate` 或 `WebClient.Builder` 上。

作用是：

```text
让它具备根据服务名进行客户端负载均衡的能力
```

示例：

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

然后可以这样调用：

```java
restTemplate.getForObject(
        "http://user-service/user/1",
        String.class
);
```

这里的 `user-service` 会被解析成真实服务实例地址。

---

## 38. `lb://user-service` 是什么意思？

在 Spring Cloud Gateway 中：

```yaml
uri: lb://user-service
```

表示：

```text
通过负载均衡器从 user-service 的多个实例中选择一个实例进行转发
```

常见于：

```text
Spring Cloud Gateway + 服务注册中心 + Spring Cloud LoadBalancer
```

---

# 十二、总结

Spring Cloud LoadBalancer 和 Ribbon 都是客户端负载均衡组件，作用都是让服务调用方从注册中心获取服务实例列表，然后在本地根据负载均衡算法选择一个实例进行调用。

区别主要有几个方面。第一，所属生态不同。Ribbon 是 Netflix OSS 体系中的组件，早期 Spring Cloud Netflix 中使用较多；Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的新负载均衡组件。第二，维护状态不同。Ribbon 已经进入维护模式，新项目不推荐继续使用；Spring Cloud LoadBalancer 是当前 Spring Cloud 推荐方案。第三，扩展模型不同。Ribbon 主要通过 `ILoadBalancer`、`IRule`、`ServerList` 扩展；Spring Cloud LoadBalancer 主要通过 `ReactorLoadBalancer` 和 `ServiceInstanceListSupplier` 扩展。第四，编程模型不同。Ribbon 更偏传统阻塞式调用，而 Spring Cloud LoadBalancer 同时支持阻塞式和响应式调用，能更好地配合 `WebClient`、Spring Cloud Gateway 和新版 OpenFeign。

实际项目中，如果是新项目，一般选择 Spring Cloud LoadBalancer；如果是老项目还在使用 Ribbon，可以短期维护，但中长期建议迁移到 Spring Cloud LoadBalancer。

---

## 39. 一句话总结

> Ribbon 是 Netflix 体系下的老客户端负载均衡组件，已进入维护模式；Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的新客户端负载均衡组件，支持阻塞和响应式调用，是当前新项目更推荐的选择。
