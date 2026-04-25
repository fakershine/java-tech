# Dubbo 负载均衡总结

Dubbo 负载均衡用于在一个服务有多个 Provider 实例时，选择其中一个 Provider 发起调用。

Dubbo 是 **客户端负载均衡**：

```text
Consumer 本地拿到 Provider 列表
  ↓
根据负载均衡算法选择一个 Provider
  ↓
发起远程调用
```

Dubbo 默认使用 **加权随机负载均衡 random**。:contentReference[oaicite:0]{index=0}

---

## 1. Dubbo 负载均衡在调用链中的位置

一次 Dubbo 调用大致流程：

```text
Consumer 调用代理对象
  ↓
Directory 获取 Provider 列表
  ↓
Router 路由过滤
  ↓
LoadBalance 选择一个 Provider
  ↓
Cluster 做容错处理
  ↓
发起远程调用
```

简单理解：

```text
Router：决定哪些 Provider 可以调用
LoadBalance：从可用 Provider 中选一个
Cluster：调用失败后如何处理
```

---

## 2. 常见负载均衡策略

| 策略 | 配置值 | 核心思想 | 适用场景 |
|---|---|---|---|
| 加权随机 | `random` | 按权重随机选择 Provider | 默认通用场景 |
| 加权轮询 | `roundrobin` | 按权重轮流调用 Provider | 机器性能相近 |
| 最少活跃数 | `leastactive` | 优先调用当前活跃请求少的 Provider | 接口耗时差异大 |
| 最短响应时间 | `shortestresponse` | 优先选择响应更快的 Provider | 关注响应速度 |
| 一致性 Hash | `consistenthash` | 相同参数尽量路由到同一 Provider | 有状态请求 |
| P2C | `p2c` | 随机选两个节点，再选连接数更少的 | 大规模集群 |
| 自适应 | `adaptive` | 基于 P2C，选择负载更低的节点 | 动态负载场景 |

Dubbo 官方当前列出的内置策略包括 Weighted Random、RoundRobin、LeastActive、Shortest-Response、ConsistentHash、P2C、Adaptive。:contentReference[oaicite:1]{index=1}

---

## 3. Random 加权随机

### 实现原理

根据 Provider 权重随机选择一个实例。

例如：

```text
Provider A weight = 100
Provider B weight = 200
Provider C weight = 300
```

权重越高，被选中的概率越大。

```text
C 被选中概率最高
B 次之
A 最低
```

### 优势

- 实现简单。
- 性能好。
- 调用次数越多，分布越均匀。
- 支持权重调节。

### 劣势

- 短时间内可能不够均匀。
- 可能连续多次命中同一 Provider。

### 适用场景

- 默认通用场景。
- Provider 性能不同，需要按权重分配流量。

---

## 4. RoundRobin 加权轮询

### 实现原理

按照权重轮流选择 Provider。

```text
A -> B -> C -> A -> B -> C
```

如果权重不同，会按照权重比例分配调用次数。

Dubbo 的 RoundRobin 借鉴了 Nginx 平滑加权轮询思想，避免高权重节点在短时间内被过度集中调用。:contentReference[oaicite:2]{index=2}

### 优势

- 请求分布比较均匀。
- 支持权重。
- 适合节点性能稳定的场景。

### 劣势

- 如果某个 Provider 处理慢，仍然会被轮询到。
- 不感知实时负载。

### 适用场景

- Provider 性能接近。
- 请求耗时比较稳定。

---

## 5. LeastActive 最少活跃数

### 实现原理

Dubbo 会统计每个 Provider 当前正在处理的请求数。

```text
active 越小，说明当前越空闲
```

选择活跃数最少的 Provider。

如果多个 Provider 活跃数相同，则再按权重随机选择。

示例：

```text
Provider A active = 10
Provider B active = 3
Provider C active = 7
```

优先选择：

```text
Provider B
```

### 优势

- 能感知 Provider 当前繁忙程度。
- 慢节点活跃数会升高，自然减少流量。
- 适合接口耗时差异较大的场景。

### 劣势

- 需要统计活跃调用数。
- 实现比随机和轮询复杂。
- 极端情况下可能受统计延迟影响。

### 适用场景

- 接口耗时不稳定。
- 部分机器处理较慢。
- 需要自动避开慢节点。

---

## 6. ShortestResponse 最短响应时间

### 实现原理

根据 Provider 的响应时间选择节点，优先选择响应更快的 Provider。

如果多个节点响应时间接近，再结合权重随机选择。

### 优势

- 更关注响应速度。
- 能减少慢节点流量。
- 对接口 RT 敏感的场景更友好。

### 劣势

- 需要统计响应时间。
- 响应时间具有波动性。
- 实现成本高于随机和轮询。

### 适用场景

- 对接口响应时间敏感。
- Provider 性能差异明显。
- 希望自动选择快节点。

---

## 7. ConsistentHash 一致性 Hash

### 实现原理

根据请求参数计算 Hash，让相同参数尽量路由到同一个 Provider。

例如：

```text
userId = 1001 -> Provider A
userId = 1002 -> Provider B
userId = 1001 -> Provider A
```

### 优势

- 相同参数请求落到同一 Provider。
- 适合有状态请求。
- 节点变化时影响范围较小。

### 劣势

- 可能导致数据倾斜。
- 某些热点参数可能打爆单个 Provider。
- 不适合完全无状态且追求均匀流量的场景。

### 适用场景

- 本地缓存命中。
- 会话粘滞。
- 同一用户请求希望落到同一节点。
- 有状态服务调用。

---

## 8. P2C 负载均衡

### 实现原理

P2C 全称是：

```text
Power of Two Choices
```

核心思想：

```text
随机选择两个 Provider
  ↓
比较它们当前连接数或负载
  ↓
选择负载更低的那个
```

Dubbo 官方说明 P2C 会随机选两个节点，然后选择连接数更小的节点。:contentReference[oaicite:3]{index=3}

### 优势

- 性能开销低。
- 比完全随机更容易避开高负载节点。
- 适合大规模 Provider 集群。

### 劣势

- 不是全局最优。
- 依赖负载指标准确性。

### 适用场景

- Provider 节点较多。
- 高并发大规模服务。
- 想用较低成本获得较好负载效果。

---

## 9. Adaptive 自适应负载均衡

### 实现原理

Adaptive LoadBalance 基于 P2C 思想，在随机选择的两个节点中，选择负载更小的节点。:contentReference[oaicite:4]{index=4}

它会综合考虑节点负载情况，而不是简单随机或轮询。

### 优势

- 更智能。
- 能动态适应 Provider 负载变化。
- 适合负载波动较大的系统。

### 劣势

- 实现复杂。
- 依赖运行时指标。
- 排查问题比简单算法更复杂。

### 适用场景

- 大规模微服务。
- 节点负载变化明显。
- 希望自动选择更优节点。

---

## 10. 如何配置负载均衡

### 全局配置

```yaml
dubbo:
  consumer:
    loadbalance: roundrobin
```

Dubbo 支持在全局、接口级、方法级配置负载均衡策略。:contentReference[oaicite:5]{index=5}

---

### 服务引用配置

```java
@DubboReference(loadbalance = "leastactive")
private UserService userService;
```

---

### 服务提供方配置

```java
@DubboService(loadbalance = "roundrobin")
public class UserServiceImpl implements UserService {
}
```

---

### 方法级配置

可以对不同方法使用不同负载均衡策略：

```text
查询方法：random
耗时方法：leastactive
有状态方法：consistenthash
```

---

## 11. 如何自定义负载均衡

### 实现 LoadBalance 接口

```java
public class MyLoadBalance implements LoadBalance {

    @Override
    public <T> Invoker<T> select(
            List<Invoker<T>> invokers,
            URL url,
            Invocation invocation
    ) {
        // 自定义选择逻辑
        return invokers.get(0);
    }
}
```

---

### SPI 配置

在资源目录下创建：

```text
META-INF/dubbo/org.apache.dubbo.rpc.cluster.LoadBalance
```

内容：

```text
myloadbalance=com.demo.MyLoadBalance
```

---

### 使用

```java
@DubboReference(loadbalance = "myloadbalance")
private UserService userService;
```

---

## 12. 总结

Dubbo 负载均衡是客户端负载均衡，Consumer 会从注册中心获取 Provider 列表，经过 Router 路由过滤后，由 LoadBalance 从可用 Provider 中选择一个进行调用。

Dubbo 默认使用加权随机 `random`。常见策略包括加权随机、加权轮询、最少活跃数、最短响应时间、一致性 Hash、P2C 和自适应负载均衡。

加权随机适合通用场景；加权轮询适合节点性能接近的场景；最少活跃数适合接口耗时差异大的场景；最短响应时间适合对 RT 敏感的场景；一致性 Hash 适合有状态请求或需要本地缓存命中的场景；P2C 和 Adaptive 更适合大规模集群下的动态负载场景。

一句话总结：

```text
Dubbo 负载均衡 = Consumer 本地从 Provider 列表中选一个节点调用；
默认 random，常用 random、roundrobin、leastactive、consistenthash。
```
