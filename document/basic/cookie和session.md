# Cookie 和 Session 总结

Cookie 和 Session 都用于解决 HTTP 无状态问题。

HTTP 本身是无状态的：

```text
第一次请求
  ↓
服务器不知道第二次请求是不是同一个用户
```

所以需要通过 Cookie 或 Session 记录用户状态。

---

## 1. Cookie 是什么

Cookie 是服务器发送给浏览器，并保存在浏览器本地的一小段数据。

后续浏览器请求同一个站点时，会自动携带 Cookie。

```text
服务端返回 Set-Cookie
  ↓
浏览器保存 Cookie
  ↓
后续请求自动携带 Cookie
```

示例：

```http
Set-Cookie: token=abc123; Path=/; Max-Age=3600
```

后续请求：

```http
Cookie: token=abc123
```

---

## 2. Session 是什么

Session 是服务端保存用户状态的一种机制。

服务端为每个用户创建一个 Session，并生成一个 `sessionId` 返回给浏览器。

浏览器通常通过 Cookie 保存这个 `sessionId`。

```text
用户登录
  ↓
服务端创建 Session
  ↓
生成 sessionId
  ↓
通过 Cookie 返回给浏览器
  ↓
浏览器后续请求携带 sessionId
  ↓
服务端根据 sessionId 找到用户信息
```

---

## 3. Cookie 和 Session 的关系

常见情况下：

```text
Cookie 保存 sessionId
Session 保存用户数据
```

例如：

```text
浏览器 Cookie：
JSESSIONID=abc123

服务端 Session：
abc123 -> userId=1001
```

浏览器只保存一个标识，真正的用户信息保存在服务端。

---

## 4. Cookie 和 Session 区别

| 对比项 | Cookie | Session |
|---|---|---|
| 存储位置 | 浏览器客户端 | 服务端 |
| 存储内容 | 少量数据、token、sessionId | 用户登录状态、用户信息 |
| 安全性 | 较低，客户端可见 | 较高，数据在服务端 |
| 容量 | 较小 | 理论上取决于服务端 |
| 生命周期 | 可设置过期时间 | 由服务端控制 |
| 服务端压力 | 小 | 占用服务端内存/存储 |
| 是否随请求发送 | 会自动携带 | 不会，通常靠 Cookie 中的 sessionId 关联 |
| 分布式问题 | 无明显状态共享问题 | 需要 Session 共享 |

---

## 5. Cookie 的常见属性

| 属性 | 说明 |
|---|---|
| `Name=Value` | Cookie 名和值 |
| `Domain` | Cookie 生效域名 |
| `Path` | Cookie 生效路径 |
| `Max-Age` | Cookie 有效时间 |
| `Expires` | Cookie 过期时间 |
| `HttpOnly` | 禁止 JavaScript 读取，防止 XSS 窃取 |
| `Secure` | 只允许 HTTPS 传输 |
| `SameSite` | 限制跨站请求携带 Cookie，防止 CSRF |

---

## 6. Cookie 的优点

- 保存在客户端，服务端压力小。
- 使用简单。
- 浏览器会自动携带。
- 适合保存少量状态信息。

---

## 7. Cookie 的缺点

- 容量有限。
- 客户端可见，安全性较低。
- 每次请求都会携带，可能增加网络开销。
- 容易受到 XSS、CSRF 等攻击影响。
- 不适合保存敏感信息。

---

## 8. Session 的优点

- 数据保存在服务端，安全性更好。
- 可以保存较复杂的用户状态。
- 客户端只保存 sessionId。
- 适合传统 Web 登录态管理。

---

## 9. Session 的缺点

- 占用服务端资源。
- 分布式部署时需要解决 Session 共享。
- 服务重启可能导致 Session 丢失。
- 不适合无状态微服务架构。

---

## 10. 分布式 Session 问题

单机部署时，Session 存在本机内存中没问题。

分布式部署时：

```text
用户第一次请求到服务器 A
  ↓
Session 存在服务器 A
  ↓
第二次请求到服务器 B
  ↓
服务器 B 找不到 Session
```

这就是分布式 Session 问题。

---

## 11. 分布式 Session 解决方案

### 1. Session 复制

多个服务器之间同步 Session。

```text
服务器 A Session 同步到服务器 B、C
```

缺点：

- 同步成本高。
- 节点多时性能差。
- 不适合大规模集群。

---

### 2. 粘性 Session

让同一个用户请求固定打到同一台服务器。

```text
用户 A 永远访问服务器 A
```

缺点：

- 负载不均衡。
- 服务器宕机后 Session 丢失。
- 扩缩容不灵活。

---

### 3. Redis 共享 Session

把 Session 存到 Redis 中。

```text
所有服务器都从 Redis 读写 Session
```

优点：

- 支持分布式。
- 扩展性好。
- 服务重启不丢失。
- 实际项目常用。

---

### 4. Token / JWT

服务端不保存 Session，登录后返回 Token。

```text
用户登录
  ↓
服务端生成 Token
  ↓
客户端保存 Token
  ↓
后续请求携带 Token
  ↓
服务端校验 Token
```

适合：

```text
前后端分离
移动端
微服务
无状态架构
```

---

## 12. Cookie + Session 登录流程

```text
1. 用户提交用户名和密码。
2. 服务端校验成功。
3. 服务端创建 Session。
4. 服务端生成 sessionId。
5. 服务端通过 Set-Cookie 返回 sessionId。
6. 浏览器保存 Cookie。
7. 后续请求自动携带 sessionId。
8. 服务端根据 sessionId 获取 Session。
9. 判断用户是否已登录。
```

---

## 13. Token 登录流程

```text
1. 用户登录成功。
2. 服务端生成 Token。
3. 返回 Token 给客户端。
4. 客户端保存 Token。
5. 后续请求在 Header 中携带 Token。
6. 服务端校验 Token。
7. 校验通过后识别用户身份。
```

常见携带方式：

```http
Authorization: Bearer token
```

---

## 14. Cookie 和 Token 区别

| 对比项 | Cookie + Session | Token / JWT |
|---|---|---|
| 状态保存 | 服务端保存 Session | 服务端可无状态 |
| 客户端保存 | sessionId | token |
| 分布式支持 | 需要 Session 共享 | 天然适合分布式 |
| 安全控制 | 服务端可主动失效 | JWT 主动失效较麻烦 |
| 适用场景 | 传统 Web | 前后端分离、微服务、移动端 |
| 服务端压力 | 较高 | 较低 |

---

## 15. 安全问题

### Cookie 安全

建议设置：

```text
HttpOnly
Secure
SameSite
```

作用：

```text
HttpOnly：防止 JS 读取 Cookie
Secure：只允许 HTTPS 传输
SameSite：减少 CSRF 风险
```

---

### Session 安全

需要防止：

```text
sessionId 被盗
Session 固定攻击
Session 过期时间过长
```

常见措施：

- 登录后重新生成 sessionId。
- 设置合理过期时间。
- 退出登录后销毁 Session。
- 敏感操作二次校验。

---

### Token 安全

需要注意：

- 使用 HTTPS。
- 设置合理过期时间。
- Refresh Token 机制。
- Token 泄露后需要黑名单或版本号机制。
- 不要在 JWT 中存敏感明文信息。

---

## 16. 总结

Cookie 是保存在浏览器客户端的数据，浏览器后续请求会自动携带；Session 是保存在服务端的用户会话数据，通常通过 Cookie 中的 `sessionId` 和客户端建立关联。

Cookie 安全性较低，不适合保存敏感信息；Session 安全性更好，但会占用服务端资源，并且在分布式环境下需要解决 Session 共享问题。

传统 Web 项目常用 Cookie + Session；前后端分离、移动端和微服务架构中更常用 Token 或 JWT。

一句话总结：

```text
Cookie 存客户端，Session 存服务端；
Cookie 常用来保存 sessionId，Session 用来保存用户登录状态。
```
