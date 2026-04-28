# Dubbo 序列化协议总结

Dubbo 序列化协议用于将 Java 对象转换成字节流，方便在网络中传输；服务端收到字节流后，再反序列化成对象。

核心作用：

```text
Java 对象
  ↓
序列化
  ↓
网络传输
  ↓
反序列化
  ↓
Java 对象
```

---

## 1. 为什么需要序列化

Dubbo 是远程调用框架，Consumer 和 Provider 不在同一个 JVM 中。

调用方法时需要传输：

```text
接口名
方法名
参数类型
参数值
返回结果
异常信息
```

这些对象不能直接在网络上传输，必须先序列化成字节流。

---

## 2. 常见序列化协议

Dubbo 支持多种序列化协议，例如 Hessian2、Fastjson2、JDK、Protobuf、Kryo、FST、Avro、Gson 等；Dubbo 3.2 默认支持 Hessian2、Fastjson2、JDK、Protocol Buffers。:contentReference[oaicite:0]{index=0}

| 序列化协议 | 特点 | 适用场景 |
|---|---|---|
| Hessian2 | Dubbo 常用默认方案，兼容性好 | 普通 Java RPC |
| Fastjson2 | JSON 系列，性能较好，可读性较好 | 对 JSON 生态友好 |
| JDK 序列化 | Java 原生，使用简单 | 学习或兼容场景 |
| Protobuf | 体积小，性能高，跨语言强 | 跨语言、高性能场景 |
| Kryo | 性能高，体积小 | Java 内部高性能调用 |
| FST | 性能较好 | Java 内部调用 |
| Avro | 跨语言，适合数据系统 | 大数据、跨语言 |
| Gson | JSON 序列化，可读性好 | 简单 JSON 场景 |

---

## 3. Hessian2

### 实现原理

Hessian2 是二进制序列化协议，Dubbo 中长期常用。

```text
Java 对象 -> Hessian2 二进制 -> 网络传输 -> 反序列化
```

Dubbo 官方文档中提到，相比 Java 序列化，Hessian2 序列化后的二进制流更小，序列化和反序列化速度也更好。:contentReference[oaicite:1]{index=1}

### 优势

- Dubbo 兼容性好。
- 使用成熟稳定。
- 二进制体积比 JDK 序列化小。
- 不需要额外定义 IDL 文件。

### 劣势

- 跨语言能力不如 Protobuf。
- 性能不一定是最优。
- 反序列化安全需要重点关注。

### 适用场景

- Dubbo Java 服务之间调用。
- 普通业务 RPC。
- 对兼容性要求较高的系统。

---

## 4. JDK 序列化

### 实现原理

基于 Java 原生 `Serializable`。

```java
public class UserDTO implements Serializable {
}
```

### 优势

- Java 原生支持。
- 使用简单。
- 不需要引入额外依赖。

### 劣势

- 性能较差。
- 序列化体积大。
- 跨语言能力差。
- 安全风险较高。

### 适用场景

- 学习测试。
- 老系统兼容。
- 不推荐生产高性能场景使用。

---

## 5. Protobuf

### 实现原理

Protobuf 需要先定义 `.proto` 文件，再生成对应语言的代码。

```proto
message User {
  int64 id = 1;
  string name = 2;
}
```

### 优势

- 性能高。
- 体积小。
- 跨语言能力强。
- 适合接口契约明确的系统。

### 劣势

- 需要维护 `.proto` 文件。
- 开发成本比 Hessian2 高。
- 字段变更要注意兼容性。

### 适用场景

- 跨语言 RPC。
- 高性能调用。
- Triple / gRPC 风格调用。
- 对传输体积敏感的系统。

---

## 6. Kryo / FST

### 实现原理

Kryo 和 FST 都是高性能 Java 序列化框架。

```text
Java 对象 -> 高性能二进制序列化 -> 网络传输
```

### 优势

- 性能高。
- 体积小。
- 适合 Java 内部服务调用。

### 劣势

- 跨语言能力弱。
- 兼容性和稳定性需要评估。
- 版本升级可能存在兼容问题。

### 适用场景

- Java 内部高性能 RPC。
- 对性能要求较高，但不要求跨语言的场景。

---

## 7. Fastjson2

### 实现原理

Fastjson2 是 JSON 序列化方案，Dubbo 3.2 默认支持 Fastjson2。:contentReference[oaicite:2]{index=2}

### 优势

- JSON 格式，可读性较好。
- 使用方便。
- 对 JSON 生态友好。

### 劣势

- 体积通常比二进制协议大。
- 性能通常不如 Protobuf、Kryo。
- 反序列化安全需要注意。

### 适用场景

- 调试友好场景。
- 对 JSON 兼容性有要求的系统。
- 非极致性能场景。

---

## 8. 如何配置序列化协议

### YAML 配置

```yaml
dubbo:
  protocol:
    name: dubbo
    serialization: hessian2
```

也可以配置为：

```yaml
dubbo:
  protocol:
    serialization: fastjson2
```

或：

```yaml
dubbo:
  protocol:
    serialization: protobuf
```

---

## 9. 序列化协议选型建议

| 场景 | 推荐 |
|---|---|
| 普通 Dubbo Java 服务调用 | Hessian2 |
| Dubbo 3.x 新项目 | Hessian2 / Fastjson2 |
| 跨语言调用 | Protobuf |
| 极致性能 Java 内部调用 | Kryo / FST |
| 学习测试 | JDK 序列化 |
| JSON 生态兼容 | Fastjson2 / Gson |

---

## 10. 注意事项

- Consumer 和 Provider 要支持相同的序列化协议。
- DTO 字段变更要注意兼容性。
- 不建议使用 JDK 序列化做高性能生产 RPC。
- 跨语言场景优先考虑 Protobuf。
- 反序列化存在安全风险，要避免接收不可信来源数据。
- 升级序列化协议时要考虑新老服务兼容；Dubbo 官方也提供了序列化协议无损升级相关说明。:contentReference[oaicite:3]{index=3}

---

## 11. 总结

Dubbo 序列化协议用于把请求参数、返回结果等 Java 对象转换成字节流进行网络传输。

常见序列化协议有 Hessian2、Fastjson2、JDK、Protobuf、Kryo、FST、Avro、Gson 等。Hessian2 是 Dubbo 中长期常用的序列化方式，兼容性和稳定性较好；JDK 序列化简单但性能差；Protobuf 性能高、体积小、跨语言能力强；Kryo 和 FST 性能较好，但更适合 Java 内部调用；Fastjson2 可读性和 JSON 生态较好。

实际选型时，普通 Java Dubbo 服务可以使用 Hessian2；跨语言调用优先 Protobuf；追求 Java 内部高性能可以考虑 Kryo 或 FST；生产环境要重点关注兼容性、性能和反序列化安全。

一句话总结：

```text
Dubbo 序列化协议 = 把 RPC 请求和响应对象转成字节流；
选型重点 = 性能、体积、兼容性、跨语言能力和安全性。
```
