# Dubbo 通信协议总结

Dubbo 通信协议用于定义 **Consumer 和 Provider 之间如何传输 RPC 请求和响应**。

注意区分：

```text
通信协议：解决数据怎么传输，例如 dubbo、triple、rest、grpc
序列化协议：解决对象怎么转字节，例如 hessian2、protobuf、json、kryo
```

---

## 1. 常见通信协议

| 协议 | 配置值 | 底层通信 | 默认端口 | 特点 |
|---|---|---|---|---|
| Dubbo2 | `dubbo` | TCP | 20880 | 高性能、长连接、适合内部 RPC |
| Triple | `tri` / `triple` | HTTP/2 | 50051 | Dubbo3 推荐，跨语言、云原生友好 |
| REST | `rest` | HTTP | 8080 | 适合对外 HTTP API |
| gRPC | `grpc` | HTTP/2 | 50051 | 跨语言、强契约、流式通信 |
| Hessian | `hessian` | HTTP | 8080 | 轻量级跨语言 RPC |

---

## 2. Dubbo2 协议

### 实现原理

Dubbo2 是 Dubbo 早期默认协议，基于 TCP 长连接通信。

```text
Consumer
  ↓
Dubbo2 编码请求
  ↓
TCP 长连接发送
  ↓
Provider 解码请求
  ↓
执行服务方法
  ↓
返回响应
```

### 优势

- 性能高。
- TCP 长连接，适合高频调用。
- 协议紧凑，传输效率高。
- 适合 Java 内部服务调用。

### 劣势

- 私有协议，跨语言能力一般。
- HTTP 网关接入不如 Triple 方便。
- 调试不如 HTTP 协议直观。

### 适用场景

- Java 内部 RPC。
- 老 Dubbo 项目。
- 高性能服务调用。

---

## 3. Triple 协议

### 实现原理

Triple 是 Dubbo3 推荐协议，基于 HTTP/2。

```text
Consumer
  ↓
Triple 编码请求
  ↓
HTTP/2 传输
  ↓
Provider 解码请求
  ↓
执行服务方法
  ↓
返回响应
```

### 优势

- 跨语言能力更好。
- 基于 HTTP/2，标准化程度高。
- 支持流式通信。
- 适合云原生、网关、Service Mesh。
- Dubbo3 新项目推荐使用。

### 劣势

- 老项目迁移有成本。
- 协议栈比 Dubbo2 更复杂。
- 对历史 Dubbo2 项目需要兼容处理。

### 适用场景

- Dubbo3 新项目。
- 跨语言调用。
- 云原生微服务。
- HTTP/2 / gRPC 生态。

---

## 4. REST / HTTP 协议

### 实现原理

基于 HTTP 通信，通过 URL、HTTP Method、JSON 等方式传输数据。

```text
GET /users/1001
POST /orders
```

### 优势

- 通用性强。
- 调试方便。
- 前端、网关、第三方系统容易接入。
- 适合对外开放 API。

### 劣势

- 性能通常不如二进制 RPC。
- HTTP 报文相对更大。
- 内部高频调用成本较高。

### 适用场景

- 对外接口。
- 前后端 HTTP API。
- 第三方系统集成。

---

## 5. gRPC 协议

### 实现原理

gRPC 基于 HTTP/2 和 Protobuf。

```text
.proto 文件
  ↓
生成客户端和服务端代码
  ↓
HTTP/2 + Protobuf 通信
```

### 优势

- 跨语言能力强。
- 性能较好。
- 支持流式通信。
- 接口契约清晰。

### 劣势

- 需要维护 `.proto` 文件。
- 对传统 Java 接口调用有一定侵入。
- 调试不如 REST 直观。

### 适用场景

- 跨语言 RPC。
- 高性能 RPC。
- 流式通信。

---

## 6. Hessian 协议

### 实现原理

Hessian 基于 HTTP 通信，使用 Hessian 序列化。

### 优势

- 使用简单。
- 基于 HTTP，穿透性好。
- 有一定跨语言能力。

### 劣势

- 性能不如 Dubbo2、Triple。
- 新项目使用较少。
- 主要用于老系统兼容。

### 适用场景

- 老系统兼容。
- 简单跨语言调用。
- Hessian 服务集成。

---

## 7. 协议配置

### Dubbo2

```yaml
dubbo:
  protocol:
    name: dubbo
    port: 20880
```

### Triple

```yaml
dubbo:
  protocol:
    name: tri
    port: 50051
```

### 多协议暴露

```yaml
dubbo:
  protocols:
    dubbo:
      name: dubbo
      port: 20880
    tri:
      name: tri
      port: 50051
```

---

## 8. 协议选型

| 场景 | 推荐协议 |
|---|---|
| Java 内部高性能 RPC | Dubbo2 |
| Dubbo3 新项目 | Triple |
| 跨语言调用 | Triple / gRPC |
| 对外 HTTP API | REST |
| 老系统兼容 | Dubbo2 / Hessian |
| 云原生、网关、Service Mesh | Triple |
| 流式通信 | Triple / gRPC |

---

## 9. 总结

Dubbo 通信协议用于定义 Consumer 和 Provider 之间如何传输 RPC 请求和响应。

Dubbo2 是基于 TCP 的高性能私有协议，适合 Java 内部服务调用；Triple 是 Dubbo3 推荐协议，基于 HTTP/2，更适合跨语言、云原生、网关和 Service Mesh 场景；REST 适合对外 HTTP API；gRPC 适合跨语言和强契约场景；Hessian 多用于老系统兼容。

一句话总结：

```text
Dubbo2 适合内部高性能 RPC；
Triple 适合 Dubbo3、跨语言和云原生；
REST 适合开放 HTTP API；
gRPC 适合跨语言强契约调用。
```
